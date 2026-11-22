# AC Gateway · Admin console

React 18 + TypeScript + Vite, animations with framer-motion. Pages: Statistics (home), Plugins, Models, Users, Audit log, plus a link to the Grafana plugins dashboard.

## Keycloak setup (once)
In realm `gateway` create a **public** client `ac-gateway-admin`:
- Standard flow on, Direct access grants off, PKCE method S256
- Valid redirect URIs: `http://localhost:8092/*` (and `http://localhost:5173/*` for dev)
- Web origins: `http://localhost:8092` (and `http://localhost:5173`), needed for the token request from the browser
- Give admin users the realm role `gateway-admin`
- Valid post logout redirect URIs: the same addresses (needed for Sign out to return to the console)
Sign-in needs a secure context: use `localhost` or HTTPS.

## Development
    npm install
    GATEWAY_URL=http://localhost:8080 npm run dev      # http://localhost:5173
Vite proxies `/api/*` to the gateway. Demo data covers the plugin pipeline only; other pages need a live gateway.

## Docker / compose
Put the project in `code/admin-ui`, add the service from `compose.admin-ui.yml`, then `docker compose up -d --build admin-ui`.
nginx serves the static build and proxies `/api/` to `gateway:8080`.
Overrides: build args `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT`.

## Plugin guide, editor and tests
- `public/PLUGINS.md` is the plugin guide and the AI prompt shown in the New plugin form (button "AI prompt"). The console loads it at runtime from `/PLUGINS.md`; mount your own file over it to update it without a rebuild.
- Code and JSON are edited with CodeMirror 6 (highlighting, folding, indentation guides, JSON linting).
- The Test tab runs plugin code in the browser with Pyodide (Python 3.12 in WebAssembly, standard library only) inside a Web Worker with a 15 s timeout.
  `src/pyrunner.ts` contains a stand-in for the Plugin Manager (`plugin_api`, class lookup, result validation, metrics) that follows PLUGINS.md.
  The runtime (about 10 MB) is downloaded from jsDelivr on the first run. For an offline setup host Pyodide yourself and set `VITE_PYODIDE_URL`.

## Grafana link
The sidebar link and the "Metrics in Grafana" item in the plugin menu open the plugins dashboard. By default the host is the one the console is opened on, port 3000;
set the full dashboard URL with the build arg / env `VITE_GRAFANA_URL` (keep `var-plugin=$__all`, it is replaced with the plugin name).
