import { useEffect, useMemo, useState } from 'react'
import CodeMirror from '@uiw/react-codemirror'
import { python } from '@codemirror/lang-python'
import { json, jsonParseLinter } from '@codemirror/lang-json'
import { linter, lintGutter } from '@codemirror/lint'
import { indentUnit } from '@codemirror/language'
import { indentWithTab } from '@codemirror/commands'
import { keymap } from '@codemirror/view'
import { indentationMarkers } from '@replit/codemirror-indentation-markers'

function useDark() {
  const [dark, setDark] = useState(document.documentElement.dataset.theme === 'dark')
  useEffect(() => {
    const o = new MutationObserver(() => setDark(document.documentElement.dataset.theme === 'dark'))
    o.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
    return () => o.disconnect()
  }, [])
  return dark
}

/** CodeMirror 6: syntax highlighting, folding, bracket matching, indentation guides, Tab to indent, JSON linting. */
export default function CodeEditor({ value, onChange, readOnly = false, lang = 'python', height }: {
  value: string; onChange: (v: string) => void; readOnly?: boolean; lang?: 'python' | 'json'; height?: number
}) {
  const dark = useDark()
  const extensions = useMemo(() => [
    lang === 'json' ? [json(), linter(jsonParseLinter()), lintGutter(), indentUnit.of('  ')] : [python(), indentUnit.of('    ')],
    indentationMarkers({ highlightActiveBlock: true, hideFirstIndent: false }),
    keymap.of([indentWithTab]),
  ], [lang])
  return (
    <div className="cm-wrap">
      <CodeMirror value={value} height={`${height ?? 380}px`} theme={dark ? 'dark' : 'light'} extensions={extensions} readOnly={readOnly}
        onChange={v => onChange(v)} aria-label={lang === 'json' ? 'JSON editor' : 'Python editor'}
        basicSetup={{ lineNumbers: true, foldGutter: true, highlightActiveLine: true, indentOnInput: true, bracketMatching: true, closeBrackets: true, autocompletion: false, tabSize: lang === 'json' ? 2 : 4 }} />
    </div>
  )
}
