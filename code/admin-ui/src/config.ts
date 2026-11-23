/** Runtime configuration, written to /config.js when the container starts (see docker/40-runtime-config.sh). */
interface RuntimeConfig { keycloakUrl?: string; keycloakRealm?: string; keycloakClient?: string; grafanaUrl?: string; apiUrl?: string; pyodideUrl?: string }
declare global { interface Window { __APP_CONFIG__?: RuntimeConfig } }

const rt: RuntimeConfig = window.__APP_CONFIG__ ?? {}
const first = (...v: unknown[]): string => { for (const x of v) if (typeof x === 'string' && x.trim()) return x.trim(); return '' }

// VITE_* values are only a fallback for `npm run dev`; the production image is built without them.
export const config = {
  keycloakUrl: first(rt.keycloakUrl, import.meta.env.VITE_KEYCLOAK_URL).replace(/\/+$/, ''),
  keycloakRealm: first(rt.keycloakRealm, import.meta.env.VITE_KEYCLOAK_REALM, 'gateway'),
  keycloakClient: first(rt.keycloakClient, import.meta.env.VITE_KEYCLOAK_CLIENT, 'ac-gateway-admin'),
  grafanaUrl: first(rt.grafanaUrl, import.meta.env.VITE_GRAFANA_URL),
  apiUrl: first(rt.apiUrl, import.meta.env.VITE_API_URL, '/api'),
  pyodideUrl: first(rt.pyodideUrl, import.meta.env.VITE_PYODIDE_URL, 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full'),
}
