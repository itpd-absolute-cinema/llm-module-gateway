import { useMemo, useState } from 'react'
import { Modal, useLoad, useToast } from './ui'
import { listModels } from '../api'
import { config } from '../config'
import { copyText } from '../clipboard'

const ACRONYMS = new Set(['gpt', 'llm', 'ai', 'api'])
/** "stub-model" -> "Stub Model", "gpt-test" -> "GPT Test" */
const title = (id: string) => id.split(/[-_\s]+/).filter(Boolean).map(w => ACRONYMS.has(w.toLowerCase()) ? w.toUpperCase() : w[0].toUpperCase() + w.slice(1)).join(' ') || id

export function buildOpenCodeConfig(o: { issuer: string; clientId: string; baseURL: string; models: string[] }) {
  return {
    $schema: 'https://opencode.ai/config.json',
    plugins: [{
      package: 'opencode-keycloak-auth',
      options: { issuer: o.issuer, clientId: o.clientId, providerId: 'keycloak', callbackPort: 49170, offlineAccess: false },
    }],
    providers: {
      keycloak: {
        name: 'AC Gateway',
        package: '@ai-sdk/openai-compatible',
        settings: { baseURL: o.baseURL },
        models: Object.fromEntries(o.models.map(id => [id, { name: title(id) }])),
      },
    },
  }
}

function Body({ onClose }: { onClose: () => void }) {
  const toast = useToast()
  const m = useLoad(async () => (await listModels()).items.map(x => x.id).sort(), [])
  const [gateway, setGateway] = useState(config.gatewayUrl || `${location.protocol}//${location.hostname}:8080`)
  const [clientId, setClientId] = useState(config.opencodeClientId)
  const text = useMemo(() => JSON.stringify(buildOpenCodeConfig({
    issuer: `${config.keycloakUrl}/realms/${config.keycloakRealm}`, clientId: clientId.trim(), baseURL: gateway.trim().replace(/\/+$/, ''), models: m.data ?? [],
  }), null, 2), [gateway, clientId, m.data])
  async function copy() {
    try { await copyText(text); toast('OpenCode config copied') } catch { toast('Could not copy. Select the text and copy it manually', true) }
  }
  return (<>
    <h2>OpenCode config</h2>
    <p className="muted">Put this into <span className="mono">opencode.json</span>. The model list is taken from the gateway.</p>
    <div className="cols">
      <div><label htmlFor="oc-gw" style={{ marginTop: 0 }}>Gateway address (as seen by the user)</label><input id="oc-gw" value={gateway} onChange={e => setGateway(e.target.value)} /></div>
      <div><label htmlFor="oc-cl" style={{ marginTop: 0 }}>Keycloak client</label><input id="oc-cl" value={clientId} onChange={e => setClientId(e.target.value)} /></div>
    </div>
    {!config.keycloakUrl && <div className="err">Keycloak address is not configured (KEYCLOAK_PUBLIC_URL), so the issuer is incomplete.</div>}
    {m.err && <div className="err">Could not load models: {m.err}</div>}
    {m.loading ? <div className="skel block" style={{ marginTop: 12 }} /> : <pre className="json tall" style={{ marginTop: 12 }} aria-label="OpenCode config">{text}</pre>}
    <div className="row"><button className="btn" onClick={onClose}>Close</button><button className="btn pri" disabled={m.loading} onClick={copy}>Copy</button></div>
  </>)
}

export default function OpenCodeConfig({ open, onClose }: { open: boolean; onClose: () => void }) {
  return <Modal open={open} onClose={onClose} wide>{open && <Body onClose={onClose} />}</Modal>
}
