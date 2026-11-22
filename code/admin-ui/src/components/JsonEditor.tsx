import { ReactNode, useState } from 'react'
import CodeEditor from './CodeEditor'

export function parseObject(text: string): { value?: Record<string, unknown>; error?: string } {
  try {
    const v = JSON.parse(text)
    if (!v || typeof v !== 'object' || Array.isArray(v)) return { error: 'A JSON object is required (curly braces)' }
    return { value: v }
  } catch (e) {
    const m = (e as Error).message, pos = /position (\d+)/.exec(m)
    if (!pos) return { error: m }
    const before = text.slice(0, +pos[1])
    return { error: `Line ${before.split('\n').length}, column ${+pos[1] - before.lastIndexOf('\n')}: ${m.replace(/ in JSON at position.*/, '').replace(/^JSON\.parse: /, '')}` }
  }
}

type Spec = { type?: unknown; description?: string; enum?: unknown[]; default?: unknown }
const specs = (schema?: Record<string, unknown>) => (schema?.properties ?? {}) as Record<string, Spec>

function Field({ name, spec, req, value, onChange }: { name: string; spec: Spec; req: boolean; value: unknown; onChange: (v: unknown) => void }) {
  const [draft, setDraft] = useState<string | null>(null)
  const ph = spec.default !== undefined ? String(spec.default) : ''
  let input: ReactNode
  if (Array.isArray(spec.enum)) {
    input = <select id={'f-' + name} value={value === undefined ? '' : JSON.stringify(value)} onChange={e => onChange(e.target.value === '' ? undefined : JSON.parse(e.target.value))}>
      <option value="">{ph ? `default (${ph})` : '—'}</option>{spec.enum.map(v => <option key={JSON.stringify(v)} value={JSON.stringify(v)}>{String(v)}</option>)}</select>
  } else if (spec.type === 'boolean') {
    input = <label className="check"><input id={'f-' + name} type="checkbox" checked={Boolean(value ?? spec.default)} onChange={e => onChange(e.target.checked)} /> Enabled</label>
  } else if (spec.type === 'integer' || spec.type === 'number') {
    input = <input id={'f-' + name} type="number" step={spec.type === 'integer' ? 1 : 'any'} placeholder={ph} value={value === undefined ? '' : String(value)} onChange={e => onChange(e.target.value === '' ? undefined : Number(e.target.value))} />
  } else if (spec.type === 'string') {
    input = <input id={'f-' + name} placeholder={ph} value={typeof value === 'string' ? value : ''} onChange={e => onChange(e.target.value === '' ? undefined : e.target.value)} />
  } else {
    input = <input id={'f-' + name} className="mono" placeholder="JSON value" value={draft ?? (value === undefined ? '' : JSON.stringify(value))}
      onChange={e => { setDraft(e.target.value); if (!e.target.value.trim()) return onChange(undefined); try { onChange(JSON.parse(e.target.value)) } catch { /* still typing */ } }}
      onBlur={() => setDraft(null)} />
  }
  return <div className="field"><label htmlFor={'f-' + name}>{name}{req ? ' *' : ''}</label>{input}{spec.description && <small className="muted">{spec.description}</small>}</div>
}

/** JSON editor for plugin options: form view generated from options_schema (when there is one) and a code view with validation. */
export default function JsonEditor({ text, onText, schema, height = 300 }: { text: string; onText: (t: string) => void; schema?: Record<string, unknown>; height?: number }) {
  const props = specs(schema), keys = Object.keys(props), hasForm = keys.length > 0
  const [mode, setMode] = useState<'form' | 'json'>(hasForm ? 'form' : 'json')
  const parsed = parseObject(text)
  const required = Array.isArray(schema?.required) ? (schema!.required as string[]) : []
  const extra = parsed.value ? Object.keys(parsed.value).filter(k => !(k in props)) : []
  const set = (k: string, v: unknown) => { const n = { ...parsed.value }; if (v === undefined) delete n[k]; else n[k] = v; onText(JSON.stringify(n, null, 2)) }
  return (
    <div className="jedit">
      <div className="jtools">
        {hasForm && <div className="seg"><button type="button" className={mode === 'form' ? 'on' : ''} onClick={() => setMode('form')}>Form</button><button type="button" className={mode === 'json' ? 'on' : ''} onClick={() => setMode('json')}>JSON</button></div>}
        <span className="grow" />
        <button type="button" className="btn" disabled={!parsed.value} onClick={() => onText(JSON.stringify(parsed.value, null, 2))}>Format</button>
        <button type="button" className="btn" disabled={!parsed.value} onClick={() => onText(JSON.stringify(parsed.value))}>Minify</button>
      </div>
      {mode === 'form' && hasForm ? (
        parsed.value ? (<div className="form">
          {keys.map(k => <Field key={k} name={k} spec={props[k]} req={required.includes(k)} value={parsed.value![k]} onChange={v => set(k, v)} />)}
          {extra.length > 0 && <p className="muted">Other keys kept as is: {extra.join(', ')}. Edit them in the JSON view.</p>}
        </div>) : <div className="err">Fix the JSON first. {parsed.error}</div>
      ) : <CodeEditor lang="json" value={text} onChange={onText} height={height} />}
      <div className={'jstatus' + (parsed.error ? ' bad' : '')}>{parsed.error ? '✗ ' + parsed.error : `✓ Valid JSON · ${Object.keys(parsed.value!).length} keys`}</div>
    </div>
  )
}
