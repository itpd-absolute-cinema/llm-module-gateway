import { useEffect, useMemo, useState } from 'react'
import { getPluginSource } from '../api'
import type { Plugin } from '../types'
import { Modal } from './ui'
import Workbench from './Workbench'
import PluginIssues from './PluginIssues'
import { analyzePlugin, withExpected } from '../plugincheck'

export default function PluginCodeModal({ plugin, onClose, onSave }: {
  plugin: Plugin | null; onClose: () => void; onSave: (name: string, source: string) => Promise<void>
}) {
  const [src, setSrc] = useState(''), [orig, setOrig] = useState('')
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading'), [err, setErr] = useState(''), [busy, setBusy] = useState(false)
  const name = plugin?.name
  useEffect(() => {
    if (!name) return
    setState('loading'); setErr('')
    getPluginSource(name).then(s => { setSrc(s); setOrig(s); setState('ready') }).catch(e => { setErr((e as Error).message); setState('error') })
  }, [name])
  const dirty = src !== orig
  const a = useMemo(() => analyzePlugin(src), [src])
  const hasError = withExpected(a, plugin?.type).some(i => i.level === 'error')
  const close = () => { if (dirty && !confirm('Discard unsaved changes?')) return; onClose() }
  async function save() { if (!name) return; setBusy(true); await onSave(name, src); setBusy(false); setOrig(src); onClose() }
  return (
    <Modal open={!!plugin} onClose={close} wide>
      <h2>{name}.py</h2>
      {plugin?.enabled && <p className="muted">This plugin is enabled. Saved code goes live for the next request.</p>}
      {plugin && (state === 'loading' ? <div className="skel block" /> : state === 'error' ? <div className="err">{err}</div>
        : <Workbench code={src} onCode={setSrc} type={plugin.type} options={JSON.stringify(plugin.options ?? {}, null, 2)} storageKey={plugin.name} optionsNote="the saved options of this plugin" />)}
      {plugin && state === 'ready' && <PluginIssues a={a} expected={plugin.type} />}
      <div className="row"><button className="btn" onClick={close}>Close</button>
        <button className="btn pri" disabled={!dirty || busy || hasError || state !== 'ready'} onClick={save}>{busy ? 'Saving…' : 'Save code'}</button></div>
    </Modal>
  )
}
