import { DragEvent, FormEvent, useCallback, useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { listPlugins, place, removePlugin, setOptions, setPluginSource, setStatus, uploadPlugin } from '../api'
import type { Plugin, PluginType } from '../types'
import { Modal, useToast } from '../components/ui'
import PluginDrawer from '../components/PluginDrawer'
import { useNavigate } from 'react-router-dom'
import { grafanaUrl } from '../grafana'
import PluginCodeModal from '../components/PluginCode'

const LANES: { type: PluginType; title: string; sub: string }[] = [
  { type: 'request_filter', title: 'Before the model request', sub: 'Process the request, top to bottom' },
  { type: 'response_filter', title: 'After the model response', sub: 'Process the response, top to bottom' },
]
type Mark = { type: PluginType; before: string } | null // before === '' means "insert at the end"

/** Fingerprint of the whole chain: order, type, state and options of every plugin. */
const fp = (l: Plugin[]) => JSON.stringify(l.map(p => [p.name, p.type, p.enabled, p.options]))
class Conflict extends Error {
  constructor() { super('The chain was changed elsewhere. The latest version is loaded and nothing was applied.'); this.name = 'Conflict' }
}

export default function Pipeline() {
  const toast = useToast()
  const [list, setList] = useState<Plugin[] | null>(null)
  const [open, setOpen] = useState<string | null>(null)
  const [menu, setMenu] = useState<{ name: string; x: number; y: number } | null>(null)
  const [drag, setDrag] = useState<string | null>(null)
  const [mark, setMark] = useState<Mark>(null)
  const nav = useNavigate()
  const [codeFor, setCodeFor] = useState<string | null>(null)
  const [conflict, setConflict] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const snap = useRef('') // fingerprint of the chain as last loaded from the server

  const load = useCallback(async () => {
    try { const l = await listPlugins(); snap.current = fp(l); setList(l) }
    catch (e) { setList(s => s ?? []); toast((e as Error).message, true) }
  }, [toast])
  useEffect(() => { load() }, [load])
  useEffect(() => {
    const close = () => setMenu(null)
    addEventListener('click', close); addEventListener('scroll', close, true)
    return () => { removeEventListener('click', close); removeEventListener('scroll', close, true) }
  }, [])

  async function refresh() {
    setRefreshing(true)
    try {
      const l = await listPlugins(), changed = fp(l) !== snap.current
      snap.current = fp(l); setList(l); setConflict(false)
      toast(changed ? 'Plugin chain updated from the server' : 'The chain is already up to date')
    } catch (e) { toast((e as Error).message, true) }
    setRefreshing(false)
  }

  /** Before any change: re-read the chain and make sure nobody has modified it since we loaded it. */
  async function guard() {
    const fresh = await listPlugins()
    if (fp(fresh) !== snap.current) { snap.current = fp(fresh); setList(fresh); setConflict(true); throw new Conflict() }
  }
  async function run(fn: () => Promise<void>, ok: string) {
    try { await guard(); await fn(); setConflict(false); toast(ok) } catch (e) { toast((e as Error).message, true) }
    await load()
  }
  const items = (t: PluginType) => (list ?? []).filter(p => p.type === t)

  // before and after are mutually exclusive: only one of them is sent
  function move(p: Plugin, before?: string, after?: string) {
    setList(l => l && place(l, p.name, before, after)) // move on screen right away, the guard reverts it on conflict
    return run(() => setStatus(p.name, { enabled: p.enabled, before, after }), 'Order updated')
  }
  const toggle = (p: Plugin) => run(() => setStatus(p.name, { enabled: !p.enabled }), p.enabled ? 'Plugin disabled. The chain passes through it' : 'Plugin enabled')

  function slot(e: DragEvent<HTMLElement>) {
    const nodes = [...e.currentTarget.querySelectorAll<HTMLElement>('[data-n]:not(.drag)')]
    const next = nodes.find(n => { const r = n.getBoundingClientRect(); return e.clientY < r.top + r.height / 2 })
    return { last: nodes[nodes.length - 1]?.dataset.n, next: next?.dataset.n }
  }
  function onOver(e: DragEvent<HTMLElement>, type: PluginType) {
    const p = list?.find(x => x.name === drag)
    if (!p || p.type !== type) return
    e.preventDefault(); setMark({ type, before: slot(e).next ?? '' })
  }
  function onDrop(e: DragEvent<HTMLElement>, type: PluginType) {
    const p = list?.find(x => x.name === drag)
    setMark(null); if (!p || p.type !== type) return
    e.preventDefault()
    const arr = items(type), i = arr.indexOf(p), { last, next } = slot(e)
    if (!last || (next ? arr[i + 1]?.name === next : i === arr.length - 1)) return
    move(p, next, next ? undefined : last)
  }
  function keys(e: React.KeyboardEvent, p: Plugin, arr: Plugin[]) {
    if (e.key === 'Enter') return setOpen(p.name)
    const i = arr.indexOf(p)
    if (e.altKey && e.key === 'ArrowUp' && arr[i - 1]) { e.preventDefault(); move(p, arr[i - 1].name) }
    if (e.altKey && e.key === 'ArrowDown' && arr[i + 1]) { e.preventDefault(); move(p, undefined, arr[i + 1].name) }
  }

  const mp = menu && list?.find(p => p.name === menu.name)
  const mArr = mp ? items(mp.type) : []

  return (
    <>
      <div className="head">
        <div>
          <h1>Plugins</h1>
          <p className="lead">Drag a plugin to change its position within its group (keyboard: Alt + arrows). Right-click a plugin for more actions. A disabled plugin does not break the chain: requests simply pass through it untouched.</p>
        </div>
        <div className="acts">
          <button className="btn" onClick={refresh} disabled={refreshing}>
            <span className={'spin' + (refreshing ? ' spinning' : '')} aria-hidden>↻</span>Refresh plugins
          </button>
          <button className="btn pri" data-tour="new-plugin" onClick={() => nav('/plugins/new')}>New plugin</button>
        </div>
      </div>

      <AnimatePresence>
        {conflict && (
          <motion.div className="banner" role="alert" initial={{ opacity: 0, height: 0, marginBottom: 0 }} animate={{ opacity: 1, height: 'auto', marginBottom: 18 }} exit={{ opacity: 0, height: 0, marginBottom: 0 }}>
            <span>The chain was changed on the server by someone else. The latest version is shown. Review it and repeat your action.</span>
            <button className="btn" onClick={() => setConflict(false)}>Dismiss</button>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="flow" data-tour="flow">
        {LANES.map((l, idx) => (
          <div key={l.type} className="col" style={{ order: idx ? 3 : 1 }}>
            <section className={'lane card' + (mark?.type === l.type && mark.before === '' ? ' end' : '')}
              onDragOver={e => onOver(e, l.type)} onDragLeave={() => setMark(null)} onDrop={e => onDrop(e, l.type)}>
              <h2>{l.title}</h2><p>{l.sub}</p>
              {list === null ? <div className="skel block" /> : items(l.type).length === 0 ? <div className="empty">No plugins yet. Upload the first one.</div> : (
                <ul className="chain"><AnimatePresence initial={false}>
                  {items(l.type).map((p, _i, arr) => (
                    <motion.li key={p.name} layout className={'slot' + (p.enabled ? ' live' : '')}
                      initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: .94 }}
                      transition={{ type: 'spring', stiffness: 520, damping: 40 }}>
                      <div role="button" tabIndex={0} draggable data-n={p.name}
                        className={'node' + (p.enabled ? '' : ' off') + (drag === p.name ? ' drag' : '') + (mark?.type === l.type && mark.before === p.name ? ' before' : '')}
                        onDragStart={e => { e.dataTransfer.setData('text/plain', p.name); e.dataTransfer.effectAllowed = 'move'; setDrag(p.name) }}
                        onDragEnd={() => { setDrag(null); setMark(null) }}
                        onClick={() => setOpen(p.name)} onKeyDown={e => keys(e, p, arr)}
                        onContextMenu={e => { e.preventDefault(); setMenu({ name: p.name, x: e.clientX, y: e.clientY }) }}>
                        <span className="dot" />
                        <div><b>{p.name}</b><small>{p.enabled ? 'Enabled' : 'Bypassed'}</small></div>
                        <span className="pill">{Object.keys(p.options ?? {}).length} opts</span>
                      </div>
                    </motion.li>
                  ))}
                </AnimatePresence></ul>
              )}
            </section>
          </div>
        ))}
        <motion.div className="llm" style={{ order: 2 }} animate={{ boxShadow: ['0 0 0 0 rgba(124,92,240,.45)', '0 0 0 14px rgba(124,92,240,0)'] }} transition={{ duration: 2.2, repeat: Infinity }}>
          LLM<small>upstream</small>
        </motion.div>
      </div>

      <AnimatePresence>
        {menu && mp && (
          <motion.div className="menu" role="menu" style={{ left: Math.min(menu.x, innerWidth - 230), top: Math.min(menu.y, innerHeight - 220) }}
            initial={{ opacity: 0, scale: .92 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: .96 }} transition={{ duration: .12 }}
            onClick={e => e.stopPropagation()}>
            <button onClick={() => { setMenu(null); toggle(mp) }}>{mp.enabled ? 'Disable (bypass)' : 'Enable'}</button>
            <button onClick={() => { setMenu(null); window.open(grafanaUrl(mp.name), '_blank', 'noopener') }}>Metrics in Grafana ↗</button>
            <button disabled={mArr[0] === mp} onClick={() => { setMenu(null); move(mp, mArr[0].name) }}>Move to top of group</button>
            <button disabled={mArr[mArr.length - 1] === mp} onClick={() => { setMenu(null); move(mp, undefined, mArr[mArr.length - 1].name) }}>Move to bottom of group</button>
            <button className="danger" disabled={mp.enabled} title={mp.enabled ? 'Only a disabled plugin can be deleted' : undefined}
              onClick={() => { setMenu(null); if (confirm(`Delete plugin “${mp.name}” and its file?`)) run(() => removePlugin(mp.name), 'Plugin deleted') }}>Delete</button>
          </motion.div>
        )}
      </AnimatePresence>

      <PluginDrawer plugin={list?.find(p => p.name === open)} onClose={() => setOpen(null)} onCode={n => { setOpen(null); setCodeFor(n) }}
        onSave={(name, o) => run(() => setOptions(name, o), 'Settings saved')} />
      <PluginCodeModal plugin={list?.find(p => p.name === codeFor) ?? null} onClose={() => setCodeFor(null)}
        onSave={(n, src) => run(() => setPluginSource(n, src), 'Plugin code saved')} />
    </>
  )
}
