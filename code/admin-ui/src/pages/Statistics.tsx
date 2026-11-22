import { FormEvent, useMemo, useState } from 'react'
import { downloadCsv, usageEvents, usageSeries, usageSummary } from '../api'
import { Pager, useLoad, useToast } from '../components/ui'
import BarChart from '../components/BarChart'
import { int, money, num, when } from '../fmt'

const RANGES = { '24h': [864e5, 'hour'], '7d': [7 * 864e5, 'day'], '30d': [30 * 864e5, 'day'] } as const
type Range = keyof typeof RANGES
type Metric = 'total_tokens' | 'cost' | 'requests'
const METRICS: [Metric, string][] = [['total_tokens', 'Tokens'], ['cost', 'Spend'], ['requests', 'Requests']]

export default function Statistics() {
  const toast = useToast()
  const [range, setRange] = useState<Range>('7d')
  const [groupBy, setGroupBy] = useState('')
  const [metric, setMetric] = useState<Metric>('total_tokens')
  const [f, setF] = useState({ user: '', model: '', status: '' })
  const [draft, setDraft] = useState({ user: '', model: '' })
  const [page, setPage] = useState(0)
  const win = () => { const to = new Date(); return { from: new Date(to.getTime() - RANGES[range][0]).toISOString(), to: to.toISOString() } }

  const top = useLoad(async () => {
    const w = { ...win(), user_id: f.user, model: f.model }
    const [s, t] = await Promise.all([usageSummary(w), usageSeries({ ...w, interval: RANGES[range][1], group_by: groupBy })])
    return { s, t }
  }, [range, groupBy, f.user, f.model])
  const ev = useLoad(() => usageEvents({ ...win(), user_id: f.user, model: f.model, status: f.status, page, size: 15 }), [range, f, page])

  const cur = top.data?.s.currency || 'USD'
  const fmt = (n: number) => metric === 'cost' ? money(n, cur) : int(n)
  const series = useMemo(() => {
    const m = new Map<string, number>()
    for (const p of top.data?.t.points ?? []) m.set(p.bucket, (m.get(p.bucket) ?? 0) + p[metric])
    const opt: Intl.DateTimeFormatOptions = range === '24h' ? { hour: '2-digit', minute: '2-digit' } : { month: 'short', day: 'numeric' }
    return [...m].sort(([a], [b]) => (a < b ? -1 : 1)).map(([k, v]) => ({ label: new Date(k).toLocaleString('en', opt), value: v }))
  }, [top.data, metric, range])
  const groups = useMemo(() => {
    const m = new Map<string, { requests: number; tokens: number; cost: number; errors: number }>()
    for (const p of top.data?.t.points ?? []) {
      const g = m.get(p.group || '—') ?? { requests: 0, tokens: 0, cost: 0, errors: 0 }
      g.requests += p.requests; g.tokens += p.total_tokens; g.cost += p.cost; g.errors += p.errors; m.set(p.group || '—', g)
    }
    return [...m].sort(([, a], [, b]) => b.cost - a.cost || b.tokens - a.tokens)
  }, [top.data])

  const apply = (e: FormEvent) => { e.preventDefault(); setPage(0); setF(x => ({ ...x, user: draft.user.trim(), model: draft.model.trim() })) }
  const s = top.data?.s
  const cards: [string, string, string][] = [
    ['Requests', int(s?.requests), s ? `${int(s.errors)} failed` : ''],
    ['Tokens', num(s?.total_tokens), s ? `${num(s.prompt_tokens)} in · ${num(s.completion_tokens)} out` : ''],
    ['Spend', money(s?.cost, cur), ''], ['Error rate', s ? (s.error_rate * 100).toFixed(1) + '%' : '—', ''],
    ['Avg latency', s ? Math.round(s.avg_latency_ms) + ' ms' : '—', ''],
  ]
  async function exportCsv() { try { await downloadCsv({ ...win(), user_id: f.user, model: f.model, status: f.status }) } catch (e) { toast((e as Error).message, true) } }

  return (
    <>
      <div className="head">
        <div><h1>Statistics</h1><p className="lead">Tokens, spend and errors, with breakdowns by user and model.</p></div>
        <button className="btn" onClick={exportCsv}>Export CSV</button>
      </div>
      <form className="filters" onSubmit={apply}>
        <div><label>Period</label><div className="seg">{(Object.keys(RANGES) as Range[]).map(r => <button type="button" key={r} className={r === range ? 'on' : ''} onClick={() => { setRange(r); setPage(0) }}>{r}</button>)}</div></div>
        <div><label htmlFor="gb">Group by</label><select id="gb" value={groupBy} onChange={e => setGroupBy(e.target.value)}><option value="">None</option><option value="user">User</option><option value="model">Model</option></select></div>
        <div><label htmlFor="fu">User id</label><input id="fu" value={draft.user} onChange={e => setDraft({ ...draft, user: e.target.value })} placeholder="All users" /></div>
        <div><label htmlFor="fm">Model</label><input id="fm" value={draft.model} onChange={e => setDraft({ ...draft, model: e.target.value })} placeholder="All models" /></div>
        <button className="btn pri">Apply</button>
      </form>
      {top.err && <div className="card state danger-text mb">{top.err}</div>}
      <div className="stats">{cards.map(([l, v, h]) => <div key={l} className="card stat"><b>{top.loading ? <span className="skel" /> : v}</b><span>{l}</span>{h && <small>{h}</small>}</div>)}</div>
      <div className="card mb">
        <div className="head"><h2>Over time</h2><div className="seg">{METRICS.map(([k, l]) => <button key={k} className={k === metric ? 'on' : ''} onClick={() => setMetric(k)}>{l}</button>)}</div></div>
        {top.loading ? <div className="skel block" /> : <BarChart data={series} format={fmt} />}
      </div>
      {groupBy && !top.loading && (
        <div className="card mb scroll"><h2>By {groupBy}</h2>
          <table className="table"><thead><tr><th>{groupBy === 'user' ? 'User id' : 'Model'}</th><th>Requests</th><th>Errors</th><th>Tokens</th><th>Spend</th></tr></thead>
            <tbody>{groups.map(([g, v]) => <tr key={g}><td><b>{g}</b></td><td>{int(v.requests)}</td><td>{int(v.errors)}</td><td>{int(v.tokens)}</td><td>{money(v.cost, cur)}</td></tr>)}</tbody></table>
          {!groups.length && <div className="state">No data</div>}
        </div>
      )}
      <div className="card scroll">
        <div className="head"><h2>Requests</h2>
          <select aria-label="Status" value={f.status} onChange={e => { setPage(0); setF({ ...f, status: e.target.value }) }}>
            <option value="">All statuses</option><option value="ok">ok</option><option value="error">error</option><option value="blocked">blocked</option><option value="rejected_by_plugin">rejected_by_plugin</option>
          </select></div>
        {ev.err && <div className="state danger-text">{ev.err}</div>}
        {ev.loading && !ev.data ? <div className="skel block" /> : (
          <table className="table"><thead><tr><th>Time</th><th>User</th><th>Model</th><th>Tokens (in / out)</th><th>Cost</th><th>Latency</th><th>Status</th></tr></thead>
            <tbody>{ev.data?.items.map(e => (
              <tr key={e.request_id}><td>{when(e.timestamp)}</td><td className="mono">{e.user_id || '—'}</td><td>{e.model || '—'}</td>
                <td>{int(e.prompt_tokens)} / {int(e.completion_tokens)}</td><td>{money(e.cost, e.currency || cur)}</td><td>{e.latency_ms} ms</td>
                <td><span className={'pill2' + (e.status === 'ok' ? '' : ' bad')}>{e.status}{e.error_code ? ` · ${e.error_code}` : ''}</span></td></tr>
            ))}</tbody></table>
        )}
        {ev.data && !ev.data.items.length && <div className="state">No requests match these filters</div>}
        {ev.data && <Pager page={ev.data.page} size={ev.data.size} total={ev.data.total} onPage={setPage} />}
      </div>
    </>
  )
}
