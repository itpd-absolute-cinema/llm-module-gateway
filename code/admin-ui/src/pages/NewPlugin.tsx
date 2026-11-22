import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { uploadPlugin } from '../api'
import type { PluginType } from '../types'
import { useLoad, useToast } from '../components/ui'
import Workbench from '../components/Workbench'
import Markdown from '../components/Markdown'
import { buildPrompt, fetchGuide } from '../guide'
import { analyzePlugin } from '../plugincheck'
import PluginIssues from '../components/PluginIssues'

const MAX = 1024 * 1024 // the gateway rejects plugin sources over 1 MiB

function GuidePanel({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const g = useLoad(fetchGuide, [])
  const [task, setTask] = useState('')
  async function copy() {
    if (!g.data) return
    try { await navigator.clipboard.writeText(buildPrompt(g.data, task)); toast('Prompt copied. Paste it into your AI chat') }
    catch { toast('Could not access the clipboard', true) }
  }
  return (
    <motion.aside className="np-guide" initial={{ opacity: 0, x: 30 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 30 }} transition={{ duration: .2 }}>
      <div className="head"><h2 style={{ margin: 0 }}>AI prompt and plugin guide</h2><button type="button" className="btn" onClick={onClose}>Close</button></div>
      <p className="muted">The prompt is the guide below plus your task. Copy it into an AI chat, then paste the answer into the editor.</p>
      <label htmlFor="ai-task">What should the plugin do? (optional)</label>
      <textarea id="ai-task" style={{ minHeight: 80, flex: 'none' }} value={task} onChange={e => setTask(e.target.value)} placeholder="For example: reject requests longer than N characters, N is an option" />
      <div className="row" style={{ justifyContent: 'flex-start' }}><button type="button" className="btn pri" disabled={!g.data} onClick={copy}>Copy AI prompt</button></div>
      {g.err && <div className="err">{g.err}</div>}
      {g.loading && !g.data ? <div className="skel block" /> : g.data && <Markdown source={g.data} />}
    </motion.aside>
  )
}

export default function NewPlugin() {
  const nav = useNavigate(), toast = useToast()
  const [name, setName] = useState(''), [type, setType] = useState<PluginType>('request_filter'), [opts, setOpts] = useState('{}')
  const [code, setCode] = useState(''), [err, setErr] = useState(''), [busy, setBusy] = useState(false), [guide, setGuide] = useState(false)
  const pick = useRef<HTMLInputElement>(null)
  const a = useMemo(() => analyzePlugin(code), [code])
  useEffect(() => { if (a.type) setType(a.type) }, [a.type]) // the type follows the base class used in the code

  const leave = () => { if (code.trim() && !confirm('Discard this plugin?')) return; nav('/plugins') }
  async function load(f?: File) {
    if (!f) return
    setCode(await f.text()); if (!name) setName(f.name.replace(/\.py$/, ''))
  }
  async function submit(e: FormEvent) {
    e.preventDefault()
    const n = name.trim()
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(n)) return setErr('Name must be a valid Python module name: letters, digits and underscores, not starting with a digit')
    if (!code.trim()) return setErr('The plugin code is empty')
    const bad = a.issues.find(i => i.level === 'error')
    if (bad) return setErr(bad.text)
    const file = new File([code], `${n}.py`, { type: 'text/x-python' })
    if (file.size > MAX) return setErr('Plugin source exceeds the 1 MiB limit')
    try { const o = JSON.parse(opts); if (!o || typeof o !== 'object' || Array.isArray(o)) throw new Error() } catch { return setErr('Options must be a JSON object') }
    setBusy(true); setErr('')
    try { await uploadPlugin(file, type, opts); toast('Plugin created. It is disabled, so the chain passes through it until you enable it'); nav('/plugins') }
    catch (x) { setErr((x as Error).message); setBusy(false) }
  }
  const kb = (new Blob([code]).size / 1024).toFixed(1)
  return (
    <>
      <div className="head">
        <div><h1>New plugin</h1><p className="lead">Paste or write the code, or load a .py file into the editor. The plugin is created disabled, so the chain passes through it until you enable it.</p></div>
        <button className="btn" onClick={leave}>Back to plugins</button>
      </div>
      <div className={'np-page' + (guide ? ' with-guide' : '')}>
        <form className="np-main" onSubmit={submit}>
          <div className="card mb">
            <div className="cols">
              <div data-tour="np-name"><label htmlFor="pn" style={{ marginTop: 0 }}>Plugin name</label><input id="pn" value={name} onChange={e => setName(e.target.value)} placeholder="my_plugin" autoComplete="off" /></div>
              <div data-tour="np-type"><label htmlFor="pt" style={{ marginTop: 0 }}>Type</label>
                <select id="pt" value={type} disabled={!!a.type} onChange={e => setType(e.target.value as PluginType)}>
                  <option value="request_filter">Request filter (before the model)</option><option value="response_filter">Response filter (after the model)</option></select><small className="muted">{a.type ? 'Detected from the code' : 'Detected automatically once the code has a plugin class'}</small></div>
            </div>
            <div data-tour="np-options"><label htmlFor="po">Options (JSON)</label><input id="po" value={opts} onChange={e => setOpts(e.target.value)} />
            <small className="muted">Used as the plugin settings and by the Test tab.</small></div>
          </div>
          <div className="card mb" data-tour="np-code">
            <div className="head" style={{ marginBottom: 10 }}>
              <h2 style={{ margin: 0 }}>Code (Python)</h2>
              <div className="acts" style={{ alignItems: 'center' }}><span className="muted">{kb} KiB / 1024</span>
                <button type="button" data-tour="np-ai" className={'btn' + (guide ? ' on' : '')} onClick={() => setGuide(v => !v)}>AI prompt</button>
                <button type="button" className="btn" onClick={() => pick.current?.click()}>Load from file…</button></div>
            </div>
            <input ref={pick} type="file" accept=".py,text/x-python" hidden onChange={e => { load(e.target.files?.[0]); e.target.value = '' }} />
            <Workbench code={code} onCode={setCode} type={type} options={opts} storageKey={name.trim() || 'new'} template />
            {code.trim() && <PluginIssues a={a} />}
          </div>
          <div className="err">{err}</div>
          <div className="row" style={{ justifyContent: 'flex-start' }}>
            <button className="btn pri" data-tour="np-submit" disabled={busy}>{busy ? 'Creating…' : 'Create plugin'}</button><button type="button" className="btn" onClick={leave}>Cancel</button></div>
        </form>
        <AnimatePresence>{guide && <GuidePanel onClose={() => setGuide(false)} />}</AnimatePresence>
      </div>
    </>
  )
}
