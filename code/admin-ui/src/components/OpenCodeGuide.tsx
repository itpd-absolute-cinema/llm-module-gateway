import { Modal } from './ui'

export default function OpenCodeGuide({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose}>
      <h2>OpenCode setup instructions</h2>
      <p className="muted">Requires OpenCode v2.0 or newer.</p>
      <ol className="steps">
        <li>Click <b>OpenCode config</b> and copy the config.</li>
        <li>Add it to <span className="mono">~/.config/opencode/opencode.json</span>. Create the file if it does not exist.</li>
        <li>Start OpenCode and sign in: run <span className="mono">/connect</span> and choose <b>AC Gateway</b>.</li>
        <li>Select <b>Keycloak</b>, then <b>Browser (PKCE, auto-capture)</b> (recommended). Finish the sign-in in the browser window that opens.</li>
      </ol>
      <div className="row"><button className="btn pri" onClick={onClose}>Close</button></div>
    </Modal>
  )
}
