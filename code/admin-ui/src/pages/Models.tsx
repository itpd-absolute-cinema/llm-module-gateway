import { useState } from 'react'
import { getPricing, listModels, putPricing } from '../api'
import type { PriceItem } from '../types'
import { useLoad, useToast } from '../components/ui'

export default function Models() {
  const toast = useToast()
  const d = useLoad(async () => ({ models: (await listModels()).items, pt: await getPricing() }), [])
  const [edit, setEdit] = useState<Record<string, { i: string; o: string }>>({})
  const [busy, setBusy] = useState(false)
  const cur = d.data?.pt.currency || 'USD'
  const price = (id: string) => {
    const m = d.data?.models.find(x => x.id === id), t = d.data?.pt.items.find(x => x.model === id)
    return { i: t?.input_price_per_1m ?? m?.pricing?.input_price_per_1m, o: t?.output_price_per_1m ?? m?.pricing?.output_price_per_1m }
  }
  const dirty = Object.keys(edit).length > 0

  async function save() {
    if (!d.data) return
    const next = new Map<string, PriceItem>(d.data.pt.items.map(x => [x.model, x]))
    for (const [id, v] of Object.entries(edit)) {
      const i = Number(v.i), o = Number(v.o)
      if (!(i >= 0) || !(o >= 0) || v.i === '' || v.o === '') return toast(`Enter valid prices for ${id}`, true)
      next.set(id, { model: id, input_price_per_1m: i, output_price_per_1m: o, currency: cur })
    }
    setBusy(true)
    try { await putPricing([...next.values()]); toast('Prices saved. Past events keep their original cost'); setEdit({}); d.reload() }
    catch (e) { toast((e as Error).message, true) }
    setBusy(false)
  }
  const field = (id: string, k: 'i' | 'o') => {
    const p = price(id), shown = edit[id]?.[k] ?? String(k === 'i' ? p.i ?? '' : p.o ?? '')
    return <input type="number" min="0" step="any" aria-label={`${k === 'i' ? 'Input' : 'Output'} price for ${id}`} value={shown}
      onChange={e => setEdit({ ...edit, [id]: { i: edit[id]?.i ?? String(p.i ?? ''), o: edit[id]?.o ?? String(p.o ?? ''), [k]: e.target.value } })} />
  }
  return (
    <>
      <div className="head">
        <div><h1>Models</h1><p className="lead">Models served by the upstream LLM and what each token costs. Prices are per 1M tokens in {cur}.</p></div>
        <div className="acts"><button className="btn" onClick={d.reload}>Refresh</button>
          <button className="btn pri" disabled={!dirty || busy} onClick={save}>{busy ? 'Saving…' : 'Save prices'}</button></div>
      </div>
      {d.err && <div className="card state danger-text mb">{d.err}</div>}
      <div className="card scroll">
        {d.loading ? <div className="skel block" /> : !d.data?.models.length ? <div className="state">The upstream returned no models</div> : (
          <table className="table">
            <thead><tr><th>Model</th><th>Owner</th><th>Input / 1M</th><th>Output / 1M</th></tr></thead>
            <tbody>{d.data.models.map(m => (
              <tr key={m.id}><td><b>{m.id}</b></td><td className="muted">{m.owned_by || '—'}</td><td className="num">{field(m.id, 'i')}</td><td className="num">{field(m.id, 'o')}</td></tr>
            ))}</tbody>
          </table>
        )}
      </div>
    </>
  )
}
