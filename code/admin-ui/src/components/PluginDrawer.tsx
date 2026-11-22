import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import type { Plugin } from '../types'
import JsonEditor, { parseObject } from './JsonEditor'

const typeName = { request_filter: 'Request filter', response_filter: 'Response filter' }
const pretty = (p?: Plugin) => JSON.stringify(p?.options ?? {}, null, 2)

export default function PluginDrawer({ plugin, onClose, onCode, onSave }: {
  plugin?: Plugin; onClose: () => void; onCode?: (name: string) => void; onSave: (name: string, options: Record<string, unknown>) => Promise<void>
}) {
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { if (plugin) setText(pretty(plugin)) }, [plugin?.name]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    addEventListener('keydown', h); return () => removeEventListener('keydown', h)
  }, [onClose])
  const parsed = parseObject(text)
  async function save() {
    if (!parsed.value || !plugin) return
    setBusy(true); await onSave(plugin.name, parsed.value); setBusy(false); onClose()
  }
  return (
    <AnimatePresence>
      {plugin && (
        <motion.aside className="drawer" aria-label="Plugin settings"
          initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }} transition={{ type: 'spring', stiffness: 380, damping: 38 }}>
          <h2>{plugin.name}</h2>
          <div className="muted">{typeName[plugin.type]} · {plugin.enabled ? 'enabled' : 'disabled, the chain passes through it'}</div>
          {plugin.description && <p>{plugin.description}</p>}
          <label>Options. Saving replaces them entirely</label>
          <JsonEditor key={plugin.name} text={text} onText={setText} schema={plugin.options_schema} />
          {plugin.options_schema && Object.keys(plugin.options_schema).length > 0 && (
            <details><summary>Options schema</summary><pre className="json">{JSON.stringify(plugin.options_schema, null, 2)}</pre></details>)}
          <div className="row"><button className="btn" onClick={() => onCode?.(plugin.name)}>Edit code</button><button className="btn" onClick={onClose}>Close</button>
            <button className="btn pri" disabled={busy || !parsed.value} onClick={save}>{busy ? 'Saving…' : 'Save settings'}</button></div>
        </motion.aside>
      )}
    </AnimatePresence>
  )
}
