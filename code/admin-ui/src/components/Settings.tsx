import { useState } from 'react'
import { Modal } from './ui'
import { getCfg, saveCfg } from '../api'

export default function Settings({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [c, setC] = useState(getCfg())
  const save = () => { saveCfg(c); location.reload() }
  return (
    <Modal open={open} onClose={onClose}>
      <h2>Gateway connection</h2>
      <p className="muted">Normally you sign in with Keycloak. A manual token overrides it and is stored in this browser.</p>
      <label className="check"><input type="checkbox" checked={c.demo} onChange={e => setC({ ...c, demo: e.target.checked })} /> Demo data (no gateway)</label>
      <label htmlFor="u">API address</label>
      <input id="u" value={c.url} disabled={c.demo} onChange={e => setC({ ...c, url: e.target.value })} placeholder="/api" />
      <label htmlFor="t">Manual token (optional)</label>
      <input id="t" type="password" value={c.token} disabled={c.demo} onChange={e => setC({ ...c, token: e.target.value })} />
      <div className="row"><button className="btn" onClick={onClose}>Cancel</button><button className="btn pri" onClick={save}>Save</button></div>
    </Modal>
  )
}
