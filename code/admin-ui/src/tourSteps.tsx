import type { ReactNode } from 'react'

export type Chapter = 'Overview' | 'Menu' | 'Plugins'
export interface Step {
  id: string
  ch: Chapter
  title: string
  body: ReactNode
  path?: string      // page to open for this step
  target?: string    // data-tour value of the element to highlight
  wide?: boolean     // wider card for steps with code
}
export const CHAPTERS: Chapter[] = ['Overview', 'Menu', 'Plugins']

const Code = ({ children }: { children: string }) => <pre className="json tour-pre"><code>{children}</code></pre>
const K = ({ children }: { children: ReactNode }) => <code className="tour-k">{children}</code>

export const STEPS: Step[] = [
  // ───────────── Overview
  { id: 'welcome', ch: 'Overview', title: 'Welcome to AC Gateway',
    body: <>
      <p>AC Gateway sits between your apps and the LLM. Here you watch usage, manage users and prices, and build plugins that change requests and responses.</p>
      <p className="muted">This tour takes a few minutes. Restart it any time with <b>Onboarding</b> in the menu. Use ← → and Esc.</p>
    </> },
  { id: 'how', ch: 'Overview', title: 'How a request travels',
    body: <>
      <div className="tour-flow">
        <span>Client</span><i>→</i><span className="p">Request plugins</span><i>→</i><span className="m">LLM</span><i>→</i><span className="p">Response plugins</span><i>→</i><span>Client</span>
      </div>
      <p>Plugins run one after another, in the order shown on the Plugins page.</p>
    </> },

  // ───────────── Menu
  { id: 'statistics', ch: 'Menu', title: 'Statistics', path: '/statistics', target: 'nav-statistics',
    body: <p>Requests, tokens, spend and errors by period, user and model. You can browse single events and export CSV.</p> },
  { id: 'plugins', ch: 'Menu', title: 'Plugins', path: '/plugins', target: 'nav-plugins',
    body: <p>Your plugin chain. Drag to reorder, click to edit options, right-click for more. A disabled plugin is simply bypassed.</p> },
  { id: 'models', ch: 'Menu', title: 'Models', path: '/models', target: 'nav-models',
    body: <p>Models of the upstream LLM and their price per 1M input and output tokens. New prices apply only to future requests.</p> },
  { id: 'users', ch: 'Menu', title: 'Users', path: '/users', target: 'nav-users',
    body: <p>Create users, check their 30-day usage, and block or unblock them. A block takes effect on the next request. The <b>Setup</b> tab generates the OpenCode config.</p> },
  { id: 'audit', ch: 'Menu', title: 'Audit log', path: '/audit', target: 'nav-audit',
    body: <p>Who changed plugins, users and prices. Click a row to see the state before and after.</p> },
  { id: 'grafana', ch: 'Menu', title: 'Grafana', target: 'nav-grafana',
    body: <p>Dashboards with gateway health and the custom metrics of your plugins. It opens in a new tab.</p> },
  { id: 'profile', ch: 'Menu', title: 'Profile', path: '/profile', target: 'profile',
    body: <p>Your account, roles and session. Sign out and restart this onboarding here.</p> },
  { id: 'topbar', ch: 'Menu', title: 'Theme and connection', path: '/statistics', target: 'top-tools',
    body: <p>Switch between light and dark theme. The connection button shows demo or live data and opens connection settings.</p> },
  { id: 'restart', ch: 'Menu', title: 'Restart any time', target: 'nav-tour',
    body: <p>This button starts the tour again. Next: how to create a plugin.</p> },

  // ───────────── Plugins
  { id: 'p-what', ch: 'Plugins', title: 'What a plugin is', path: '/plugins', target: 'flow',
    body: <p>One Python file that changes a request <b>before</b> the model (<K>RequestFilter</K>) or a response <b>after</b> it (<K>ResponseFilter</K>), can reject data, and can publish metrics.</p> },
  { id: 'p-new', ch: 'Plugins', title: '1. New plugin', path: '/plugins', target: 'new-plugin',
    body: <p>Start here. A new plugin is created <b>disabled</b>, so nothing changes for users until you enable it.</p> },
  { id: 'p-name', ch: 'Plugins', title: '2. Name', path: '/plugins/new', target: 'np-name',
    body: <p>Latin letters, digits and <K>_</K>, not starting with a digit, e.g. <K>mask_phone</K>. It must be unique and cannot be renamed.</p> },
  { id: 'p-type', ch: 'Plugins', title: '3. Type', path: '/plugins/new', target: 'np-type',
    body: <p>Filled in automatically from the base class in your code, and cannot be changed after creation.</p> },
  { id: 'p-code', ch: 'Plugins', title: '4. Code: request filter', path: '/plugins/new', target: 'np-code', wide: true,
    body: <>
      <p>Import the base class, define <b>one</b> class and return the data from <K>apply</K>:</p>
      <Code>{`from plugin_api import RequestFilter


class MyPlugin(RequestFilter):
    def apply(self, request: dict) -> dict:
        return request`}</Code>
      <p className="muted"><b>Insert template</b> gives you a ready skeleton.</p>
    </> },
  { id: 'p-resp', ch: 'Plugins', title: '5. Code: response filter', path: '/plugins/new', target: 'np-code', wide: true,
    body: <>
      <p>A response filter also gets the original request (read-only). It does not run for streaming requests.</p>
      <Code>{`from plugin_api import ResponseFilter


class MyPlugin(ResponseFilter):
    def apply(self, response: dict, request: dict) -> dict:
        return response`}</Code>
    </> },
  { id: 'p-do', ch: 'Plugins', title: '6. Changing data', path: '/plugins/new', target: 'np-code', wide: true,
    body: <>
      <p>Edit messages, switch the model or limit parameters. Keep unknown fields and never touch <K>stream</K>.</p>
      <Code>{`for m in request["messages"]:
    m["content"] = m["content"].replace("secret", "[REDACTED]")
request["max_tokens"] = min(request.get("max_tokens") or 1000, 1000)`}</Code>
    </> },
  { id: 'p-opts', ch: 'Plugins', title: '7. Options', path: '/plugins/new', target: 'np-options', wide: true,
    body: <>
      <p>Settings as a JSON object, read through <K>self.options</K>. Always give a default.</p>
      <Code>{`mask = self.options.get("mask", "[PHONE]")`}</Code>
      <p className="muted">Add <K>description</K> and <K>options_schema</K> to the class to get a settings form.</p>
    </> },
  { id: 'p-reject', ch: 'Plugins', title: '8. Rejecting', path: '/plugins/new', target: 'np-code', wide: true,
    body: <>
      <p>Stop the pipeline with an error. Any other exception just skips the plugin.</p>
      <Code>{`from plugin_api import InvalidData, Forbidden

raise InvalidData("Message is too long")  # HTTP 400
raise Forbidden("Model not allowed")      # HTTP 403`}</Code>
    </> },
  { id: 'p-fail', ch: 'Plugins', title: '9. Time limit', path: '/plugins/new', target: 'np-options',
    body: <p>A plugin has 5 seconds by default. Change it with <K>{'{"timeout_seconds": 20}'}</K> (max 600). A slow or failing plugin is <b>skipped</b>, so data passes on unchecked.</p> },
  { id: 'p-metrics', ch: 'Plugins', title: '10. Metrics', path: '/plugins/new', target: 'np-code', wide: true,
    body: <>
      <p>Declare metrics, then record them. They appear in Grafana.</p>
      <Code>{`class MyPlugin(RequestFilter):
    metrics_spec = {"masked": {"type": "counter"}}

    def apply(self, request: dict) -> dict:
        self.metrics.inc("masked")
        return request`}</Code>
    </> },
  { id: 'p-test', ch: 'Plugins', title: '11. Test', path: '/plugins/new', target: 'wb-tabs',
    body: <p>Open the <b>Test</b> tab to run the plugin in your browser on a sample request. You see the result, errors and metrics. Nothing reaches the gateway.</p> },
  { id: 'p-ai', ch: 'Plugins', title: '12. AI helper', path: '/plugins/new', target: 'np-ai',
    body: <p>Opens the plugin guide. Describe your task, copy the AI prompt into a chat and paste the answer into the editor.</p> },
  { id: 'p-create', ch: 'Plugins', title: '13. Create', path: '/plugins/new', target: 'np-submit',
    body: <p>Press <b>Create plugin</b>. Any problems are shown right above the button.</p> },
  { id: 'p-run', ch: 'Plugins', title: '14. Enable and order', path: '/plugins', target: 'flow',
    body: <p>Right-click the new plugin and choose <b>Enable</b>, then drag it to the right place. Order matters: mask data before checking it.</p> },
  { id: 'done', ch: 'Plugins', title: 'You are ready',
    body: <p>That is all. Run this tour again any time from <b>Onboarding</b> in the menu or from your Profile.</p> },
]
