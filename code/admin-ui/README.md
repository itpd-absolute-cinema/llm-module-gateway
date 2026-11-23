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

The image is generic and contains no environment-specific addresses. When the container starts, `docker/40-runtime-config.sh` writes `/config.js` from environment variables; `index.html` loads it before the app, and nginx serves it with `Cache-Control: no-store`.
Change `.env` and recreate the container (`docker compose up -d --force-recreate admin-ui`); no rebuild is needed.

| Variable | Default | Meaning |
| --- | --- | --- |
| `KEYCLOAK_PUBLIC_URL` | required | Keycloak as seen from the user's browser, e.g. `https://keycloak.example.com` |
| `KEYCLOAK_REALM` | `gateway` | Realm |
| `KEYCLOAK_CLIENT` | `ac-gateway-admin` | Public client id |
| `GRAFANA_PUBLIC_URL` | empty | Grafana address, or a full dashboard URL. Empty: the host the console is opened on, port 3000 |
| `API_URL` | `/api` | Gateway API address (compose passes it from `ADMIN_UI_API_URL`) |
| `PYODIDE_URL` | jsDelivr | Self-hosted Pyodide for the Test tab |

The container refuses to start with a clear message if `KEYCLOAK_PUBLIC_URL` is missing or is not an http(s) URL.
For `npm run dev` the Keycloak address comes from `.env.development` (`VITE_KEYCLOAK_URL`); `VITE_*` variables are not used by the Docker build.

## Plugin guide, editor and tests

- `public/PLUGINS.md` is the plugin guide and the AI prompt shown in the New plugin form (button "AI prompt"). The console loads it at runtime from `/PLUGINS.md`; mount your own file over it to update it without a rebuild.
- Code and JSON are edited with CodeMirror 6 (highlighting, folding, indentation guides, JSON linting).
- The Test tab runs plugin code in the browser with Pyodide (Python 3.12 in WebAssembly, standard library only) inside a Web Worker with a 15 s timeout.

  `src/pyrunner.ts` contains a stand-in for the Plugin Manager (`plugin_api`, class lookup, result validation, metrics) that follows PLUGINS.md.
  The runtime (about 10 MB) is downloaded from jsDelivr on the first run. For an offline setup host Pyodide yourself and set `PYODIDE_URL`.

## Grafana link

The sidebar link and the "Metrics in Grafana" item in the plugin menu open the plugins dashboard. By default the host is the one the console is opened on, port 3000;
set `GRAFANA_PUBLIC_URL` (a Grafana address, or the full dashboard URL; keep `var-plugin=$__all`, it is replaced with the plugin name).
