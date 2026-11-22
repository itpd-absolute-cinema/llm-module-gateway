import type { PluginType } from '../types'
import { Analysis, withExpected } from '../plugincheck'

export default function PluginIssues({ a, expected }: { a: Analysis; expected?: PluginType }) {
  const issues = withExpected(a, expected)
  const ok = a.className && !issues.some(i => i.level === 'error')
  return (
    <ul className="issues" aria-live="polite">
      {ok && <li className="ok">✓ {a.className} looks like a valid {a.type === 'request_filter' ? 'request' : 'response'} filter</li>}
      {issues.map(i => <li key={i.text} className={i.level}>{i.level === 'error' ? '✗' : '⚠'} {i.text}</li>)}
    </ul>
  )
}
