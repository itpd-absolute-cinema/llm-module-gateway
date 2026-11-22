// Runs plugin code in the browser with Pyodide (Python compiled to WebAssembly) inside a Web Worker.
// A worker can be terminated, so an infinite loop in a plugin cannot freeze the console.
const PYODIDE = import.meta.env.VITE_PYODIDE_URL ?? 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full'

// A stand-in for the Plugin Manager: provides the `plugin_api` module, finds the plugin class, calls apply(),
// validates the result and collects metrics. Mirrors the contract described in PLUGINS.md.
export const HARNESS = `
import sys, types, json, io, contextlib, traceback

class InvalidData(Exception):
    status = 400

class Forbidden(Exception):
    status = 403

class _Metrics:
    def __init__(self):
        self.ops = []
    def inc(self, name, value=1, **labels):
        self.ops.append(("inc", name, value, labels))
    def set(self, name, value, **labels):
        self.ops.append(("set", name, value, labels))
    def observe(self, name, value, **labels):
        self.ops.append(("observe", name, value, labels))

class _Base:
    description = ""
    options_schema = None
    metrics_spec = None
    def __init__(self, options=None):
        self.options = dict(options or {})
        self.metrics = _Metrics()

class RequestFilter(_Base):
    def apply(self, request):
        raise NotImplementedError

class ResponseFilter(_Base):
    def apply(self, response, request):
        raise NotImplementedError

_api = types.ModuleType("plugin_api")
for _n in ("InvalidData", "Forbidden", "RequestFilter", "ResponseFilter"):
    setattr(_api, _n, globals()[_n])
sys.modules["plugin_api"] = _api

def _check_request(r):
    if not isinstance(r, dict):
        return ["the request must be an object"]
    p = []
    if not isinstance(r.get("model"), str) or not r["model"]:
        p.append("model must be a non-empty string")
    m = r.get("messages")
    if not isinstance(m, list) or not m:
        p.append("messages must be a non-empty list")
    else:
        for i, x in enumerate(m):
            if not isinstance(x, dict) or x.get("role") not in ("system", "developer", "user", "assistant"):
                p.append("messages[%d].role is invalid" % i)
            elif not isinstance(x.get("content"), str):
                p.append("messages[%d].content must be a string" % i)
    t = r.get("temperature")
    if t is not None and not (isinstance(t, (int, float)) and 0 <= t <= 2):
        p.append("temperature must be between 0 and 2")
    tp = r.get("top_p")
    if tp is not None and not (isinstance(tp, (int, float)) and 0 <= tp <= 1):
        p.append("top_p must be between 0 and 1")
    for k in ("max_tokens", "max_completion_tokens"):
        v = r.get(k)
        if v is not None and not (isinstance(v, int) and v > 0):
            p.append(k + " must be greater than 0 or null")
    s = r.get("stop")
    if s is not None and not (isinstance(s, str) or (isinstance(s, list) and all(isinstance(i, str) for i in s))):
        p.append("stop must be a string, a list of strings or null")
    return p

def _check_response(r):
    if not isinstance(r, dict):
        return ["the response must be an object"]
    c = r.get("choices")
    if c is not None and not isinstance(c, list):
        return ["choices must be a list"]
    return []

def _run(source, kind, request_json, response_json, opts_json):
    out = io.StringIO()
    res = {"outcome": "ok", "result": None, "metrics": [], "problems": [], "stdout": ""}
    base = RequestFilter if kind == "request_filter" else ResponseFilter
    other = ResponseFilter if kind == "request_filter" else RequestFilter
    try:
        ns = {"__name__": "plugin"}
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
            exec(compile(source, "plugin.py", "exec"), ns)
            mine = [c for c in ns.values() if isinstance(c, type) and issubclass(c, base) and c is not base and c.__module__ == "plugin"]
            wrong = [c for c in ns.values() if isinstance(c, type) and issubclass(c, other) and c is not other and c.__module__ == "plugin"]
            if not mine:
                hint = " (the class inherits from " + other.__name__ + ": the plugin type does not match)" if wrong else ""
                raise TypeError("No class inheriting from " + base.__name__ + " found" + hint)
            if len(mine) > 1:
                raise TypeError("Define exactly one class inheriting from " + base.__name__)
            options = json.loads(opts_json)
            options.pop("timeout_seconds", None)
            inst = mine[0](options)
            request = json.loads(request_json)
            if kind == "request_filter":
                result = inst.apply(request)
            else:
                result = inst.apply(json.loads(response_json), request)
        res["result"] = result
        res["metrics"] = [list(o) for o in inst.metrics.ops]
        res["problems"] = _check_request(result) if kind == "request_filter" else _check_response(result)
        if res["problems"]:
            res["outcome"] = "invalid"
    except (InvalidData, Forbidden) as e:
        res["outcome"] = "rejected"
        res["status"] = e.status
        res["message"] = str(e)
    except BaseException:
        res["outcome"] = "error"
        res["error"] = traceback.format_exc()
    res["stdout"] = out.getvalue()
    return json.dumps(res, default=str)
`

export interface RunResult {
  outcome: 'ok' | 'rejected' | 'invalid' | 'error' | 'failed'
  result?: unknown; status?: number; message?: string; error?: string
  problems: string[]; metrics: unknown[][]; stdout: string; ms: number
}
let worker: Worker | null = null

function make(): Worker {
  const src = `
importScripts(${JSON.stringify(PYODIDE + '/pyodide.js')});
let py = null;
self.onmessage = async (e) => {
  const { id, code, kind, request, response, opts } = e.data;
  try {
    if (!py) {
      self.postMessage({ id, phase: 'loading' });
      py = await loadPyodide({ indexURL: ${JSON.stringify(PYODIDE + '/')} });
      py.runPython(${JSON.stringify(HARNESS)});
    }
    self.postMessage({ id, phase: 'running' });
    const t = performance.now();
    py.globals.set('_src', code); py.globals.set('_kind', kind); py.globals.set('_req', request); py.globals.set('_resp', response); py.globals.set('_opts', opts);
    const out = JSON.parse(py.runPython('_run(_src, _kind, _req, _resp, _opts)'));
    self.postMessage({ id, phase: 'done', out, ms: Math.round(performance.now() - t) });
  } catch (err) {
    self.postMessage({ id, phase: 'done', out: { outcome: 'failed', error: String(err), problems: [], metrics: [], stdout: '' }, ms: 0 });
  }
};`
  return new Worker(URL.createObjectURL(new Blob([src], { type: 'text/javascript' })))
}

export function runPlugin(a: { code: string; kind: string; request: string; response: string; opts: string }, onStatus: (s: string) => void, runMs = 15000): Promise<RunResult> {
  worker ??= make()
  const w = worker, id = Math.random()
  const fail = (error: string): RunResult => ({ outcome: 'failed', error, problems: [], metrics: [], stdout: '', ms: 0 })
  return new Promise(resolve => {
    const stop = (msg: string) => { w.terminate(); if (worker === w) worker = null; resolve(fail(msg)) }
    let timer = setTimeout(() => stop('Could not load the Python runtime. Check the internet connection (it is downloaded from ' + PYODIDE + ') or set VITE_PYODIDE_URL.'), 90000)
    w.onmessage = e => {
      if (e.data.id !== id) return
      if (e.data.phase === 'loading') onStatus('Loading the Python runtime (first run only)…')
      if (e.data.phase === 'running') {
        onStatus('Running…'); clearTimeout(timer)
        timer = setTimeout(() => stop(`Timed out after ${runMs / 1000} s (possible infinite loop). In the gateway the plugin would be skipped and killed.`), runMs)
      }
      if (e.data.phase === 'done') { clearTimeout(timer); resolve({ ...e.data.out, ms: e.data.ms }) }
    }
    w.onerror = e => { clearTimeout(timer); resolve(fail('Worker error: ' + e.message)) }
    w.postMessage({ id, ...a })
  })
}
