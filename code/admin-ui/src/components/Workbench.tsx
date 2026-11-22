import { useEffect, useState } from 'react'
import type { PluginType } from '../types'
import CodeEditor from './CodeEditor'
import JsonEditor, { parseObject } from './JsonEditor'
import { runPlugin, RunResult } from '../pyrunner'
import { int } from '../fmt'

// Prepared request and response formats (see PLUGINS.md). The developer edits them in the Test tab.
const REQUEST = { model: 'gpt-x', messages: [{ role: 'system', content: 'You are a helpful assistant.' }, { role: 'user', content: 'Call me on +1 415 555 0132 about the book' }], temperature: 0.7, top_p: 1.0, max_tokens: 100, max_completion_tokens: null, stop: null, stream: false }
const RESPONSE = { id: 'chatcmpl-1', object: 'chat.completion', created: 1730000000, model: 'gpt-x', choices: [{ index: 0, message: { role: 'assistant', content: 'Sure, here is the answer.' }, finish_reason: 'stop' }], usage: { prompt_tokens: 10, completion_tokens: 5, total_tokens: 15 } }
const pretty = (v: unknown) => JSON.stringify(v, null, 2)

const TEMPLATES: Record<PluginType, string> = {
  request_filter: `from plugin_api import RequestFilter


class MyPlugin(RequestFilter):
    description = "Describe what the plugin does"
    options_schema = {
        "type": "object",
        "properties": {
            "example": {"type": "string", "default": "value"},
        },
    }

    def apply(self, request: dict) -> dict:
        example = self.options.get("example", "value")
        return request
`,
  response_filter: `from plugin_api import ResponseFilter


class MyPlugin(ResponseFilter):
    description = "Describe what the plugin does"
    options_schema = {
        "type": "object",
        "properties": {
            "text": {"type": "string", "default": "AI-generated answer."},
        },
    }

    def apply(self, response: dict, request: dict) -> dict:
        text = self.options.get("text", "AI-generated answer.")
        for choice in response.get("choices") or []:
            message = choice.get("message")
            if message and message.get("content"):
                message["content"] += "\\n\\n" + text
        return response
`,
}
const stored = (k: string): { request?: string; response?: string } => { try { return JSON.parse(localStorage.getItem(k) || '{}') } catch { return {} } }

function TestPanel({ code, type, options, storageKey, note }: { code: string; type: PluginType; options: string; storageKey: string; note: string }) {
  const key = `ac-test:${storageKey}`, saved = stored(key)
  const [request, setRequest] = useState(saved.request ?? pretty(REQUEST))
  const [response, setResponse] = useState(saved.response ?? pretty(RESPONSE))
  const [running, setRunning] = useState(false), [status, setStatus] = useState(''), [err, setErr] = useState('')
  const [res, setRes] = useState<RunResult | null>(null)
  useEffect(() => { try { localStorage.setItem(key, JSON.stringify({ request, response })) } catch { /* storage unavailable */ } }, [key, request, response])
  const isReq = type === 'request_filter'

  async function run() {
    const q = parseObject(request), r = parseObject(response), o = parseObject(options)
    if (q.error) return setErr('Request: ' + q.error)
    if (!isReq && r.error) return setErr('Response: ' + r.error)
    if (o.error) return setErr('Options (JSON): ' + o.error)
    if (!code.trim()) return setErr('Write the plugin code first')
    setErr(''); setRes(null); setRunning(true); setStatus('Starting…')
    setRes(await runPlugin({ code, kind: type, request: JSON.stringify(q.value), response: JSON.stringify(r.value ?? {}), opts: JSON.stringify(o.value) }, setStatus))
    setRunning(false)
  }
  const before = isReq ? parseObject(request).value : parseObject(response).value
  const changed = res && (res.outcome === 'ok' || res.outcome === 'invalid') && JSON.stringify(res.result) !== JSON.stringify(before)
  const badge = !res ? null : res.outcome === 'ok' ? <span className="pill2">Passed</span>
    : res.outcome === 'rejected' ? <span className="pill2 bad">Rejected · HTTP {res.status}</span>
    : res.outcome === 'invalid' ? <span className="pill2 bad">Invalid result · plugin would be skipped</span>
    : res.outcome === 'error' ? <span className="pill2 bad">Plugin failed · it would be skipped</span> : <span className="pill2 bad">Could not run</span>
  return (
    <div className="test">
      <div>
        <div className="head"><label style={{ margin: 0 }}>Request body</label><button type="button" className="btn" onClick={() => setRequest(pretty(REQUEST))}>Reset sample</button></div>
        <JsonEditor text={request} onText={setRequest} height={isReq ? 200 : 130} />
        {!isReq && (<>
          <div className="head" style={{ marginTop: 12 }}><label style={{ margin: 0 }}>LLM response (input of the filter)</label><button type="button" className="btn" onClick={() => setResponse(pretty(RESPONSE))}>Reset sample</button></div>
          <JsonEditor text={response} onText={setResponse} height={170} />
        </>)}
        <p className="muted" style={{ marginTop: 12 }}>Plugin options are taken from {note}.</p>
        <div className="acts" style={{ marginTop: 12, alignItems: 'center' }}>
          <button type="button" className="btn pri" disabled={running} onClick={run}>{running ? 'Running…' : 'Run test'}</button>
          <span className="muted">{isReq ? 'Calls apply(request)' : 'Calls apply(response, request)'}</span>
        </div>
        <div className="err">{err}</div>
      </div>
      <div>
        <label style={{ marginTop: 0 }}>Output</label>
        {running && <div className="state">{status}</div>}
        {!running && !res && <div className="state">Run the test to see the result</div>}
        {res && (<>
          <p>{badge} {(res.outcome === 'ok' || res.outcome === 'invalid') && <span className="muted">{int(res.ms)} ms · {changed ? 'data was modified' : 'data is unchanged'}</span>}</p>
          {res.outcome === 'rejected' && <p>The client would receive this error: <b>{res.message}</b></p>}
          {res.problems.length > 0 && <ul className="danger-text">{res.problems.map(p => <li key={p}>{p}</li>)}</ul>}
          {(res.outcome === 'ok' || res.outcome === 'invalid') && <pre className="json tall">{pretty(res.result)}</pre>}
          {(res.outcome === 'error' || res.outcome === 'failed') && <pre className="json tall danger-text">{res.error}</pre>}
          {res.metrics.length > 0 && (<><label>Recorded metrics</label>
            <pre className="json">{res.metrics.map(m => `${m[0]} ${m[1]} ${m[2]}` + (m[3] && Object.keys(m[3] as object).length ? ' ' + JSON.stringify(m[3]) : '')).join('\n')}</pre></>)}
          {res.stdout && <><label>Printed output</label><pre className="json">{res.stdout}</pre></>}
        </>)}
      </div>
    </div>
  )
}

export default function Workbench({ code, onCode, type, options, storageKey, template = false, optionsNote = 'the Options (JSON) field above' }: {
  code: string; onCode: (c: string) => void; type: PluginType; options: string; storageKey: string; template?: boolean; optionsNote?: string
}) {
  const [tab, setTab] = useState<'code' | 'test'>('code')
  return (
    <div>
      <div className="head" style={{ marginBottom: 10 }}>
        <div className="seg" data-tour="wb-tabs">
          <button type="button" className={tab === 'code' ? 'on' : ''} onClick={() => setTab('code')}>Code</button>
          <button type="button" className={tab === 'test' ? 'on' : ''} onClick={() => setTab('test')}>Test</button>
        </div>
        {template && tab === 'code' && <button type="button" className="btn" onClick={() => { if (code.trim() && !confirm('Replace the current code with the template?')) return; onCode(TEMPLATES[type]) }}>Insert template</button>}
      </div>
      {tab === 'code' ? <CodeEditor value={code} onChange={onCode} /> : <TestPanel code={code} type={type} options={options} storageKey={storageKey} note={optionsNote} />}
    </div>
  )
}
