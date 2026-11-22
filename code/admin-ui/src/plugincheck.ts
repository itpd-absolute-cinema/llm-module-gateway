import type { PluginType } from './types'

export interface Issue { level: 'error' | 'warning'; text: string }
export interface Analysis { type?: PluginType; className?: string; issues: Issue[] }

const BASE: Record<string, PluginType> = { RequestFilter: 'request_filter', ResponseFilter: 'response_filter' }
const indentOf = (l: string) => l.length - l.trimStart().length

function splitParams(s: string): string[] {
  const out: string[] = []
  let depth = 0, cur = ''
  for (const c of s) {
    if ('([{'.includes(c)) depth++
    if (')]}'.includes(c)) depth--
    if (c === ',' && depth === 0) { out.push(cur); cur = '' } else cur += c
  }
  out.push(cur)
  return out.map(x => x.trim()).filter(Boolean)
}
function paramsFrom(text: string, from: number): string {
  let depth = 1
  for (let i = from; i < text.length; i++) {
    if (text[i] === '(') depth++
    else if (text[i] === ')' && --depth === 0) return text.slice(from, i)
  }
  return text.slice(from)
}

/** Minimal front-end check against the contract in PLUGINS.md: one plugin class and the right apply() signature. */
export function analyzePlugin(code: string): Analysis {
  if (!code.trim()) return { issues: [] }
  const lines = code.split('\n')
  const importText = [...code.matchAll(/^[ \t]*from\s+plugin_api\s+import\s+(\([^)]*\)|[^\n]*)/gm)].map(m => m[1]).join(' ')
  const alias: Record<string, string> = {}
  for (const m of importText.matchAll(/(\w+)\s+as\s+(\w+)/g)) if (m[1] in BASE) alias[m[2]] = m[1]

  const found: { name: string; base: string; start: number; end: number }[] = []
  for (let i = 0; i < lines.length; i++) {
    const m = /^class\s+([A-Za-z_]\w*)\s*\(([^)]*)\)\s*:/.exec(lines[i])
    if (!m) continue
    const bases = m[2].split(',').map(b => { const n = b.trim().split('.').pop() ?? ''; return alias[n] ?? n })
    const base = bases.find(b => b in BASE)
    if (!base) continue
    let j = i + 1
    while (j < lines.length && (!lines[j].trim() || indentOf(lines[j]) > 0)) j++
    found.push({ name: m[1], base, start: i, end: j })
  }
  if (!found.length) return { issues: [{ level: 'error', text: 'No class inheriting from RequestFilter or ResponseFilter found.' }] }
  const sameType = found.every(f => f.base === found[0].base)
  if (found.length > 1) return { type: sameType ? BASE[found[0].base] : undefined, issues: [{ level: 'error', text: `Define exactly one plugin class. Found ${found.length}: ${found.map(f => f.name).join(', ')}.` }] }

  const c = found[0], type = BASE[c.base], issues: Issue[] = []
  if (!new RegExp('\\b' + c.base + '\\b').test(importText) && !/\bimport\s+plugin_api\b/.test(code))
    issues.push({ level: 'error', text: `Import the base class: from plugin_api import ${c.base}` })

  const block = lines.slice(c.start + 1, c.end).join('\n')
  if (/^[ \t]+async\s+def\s+apply\b/m.test(block)) issues.push({ level: 'error', text: 'apply must be a regular function, not async.' })
  else {
    const d = /^[ \t]+def\s+apply\s*\(/m.exec(block)
    if (!d) issues.push({ level: 'error', text: `${c.name} must define apply(...).` })
    else {
      const params = splitParams(paramsFrom(block, d.index + d[0].length))
      const want = type === 'request_filter' ? ['self', 'request'] : ['self', 'response', 'request']
      const sig = type === 'request_filter' ? 'apply(self, request: dict) -> dict' : 'apply(self, response: dict, request: dict) -> dict'
      const names = params.map(p => p.replace(/^\*+/, '').split(/[:=]/)[0].trim())
      if (!params.some(p => p.startsWith('*'))) {
        if (params.length !== want.length) issues.push({ level: 'error', text: `apply takes ${params.length} parameter(s), expected: ${sig}` })
        else if (names[0] !== 'self') issues.push({ level: 'error', text: 'The first parameter of apply must be self.' })
        else if (names.join() !== want.join()) issues.push({ level: 'warning', text: `Expected parameter names (${want.join(', ')}), found (${names.join(', ')}).` })
      }
      if (!/\breturn\b/.test(block.slice(d.index))) issues.push({ level: 'warning', text: 'apply never returns a value. It must return the data.' })
    }
  }
  return { type, className: c.name, issues }
}

/** The analysis issues plus the check that the code still matches an existing plugin's type. */
export function withExpected(a: Analysis, expected?: PluginType): Issue[] {
  if (expected && a.type && a.type !== expected)
    return [{ level: 'error', text: `This plugin is a ${expected}, but the class inherits ${a.type === 'request_filter' ? 'RequestFilter' : 'ResponseFilter'}. The type cannot be changed after creation.` }, ...a.issues]
  return a.issues
}
