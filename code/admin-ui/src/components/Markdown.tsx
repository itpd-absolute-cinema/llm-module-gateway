import { Fragment, ReactNode } from 'react'

const slug = (s: string) => s.toLowerCase().replace(/[^a-z0-9\s-]/g, '').trim().replace(/\s+/g, '-')

function inline(s: string): ReactNode[] {
  return s.split(/(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g).map((p, i) => {
    if (p.startsWith('`') && p.endsWith('`') && p.length > 1) return <code key={i}>{p.slice(1, -1)}</code>
    if (p.startsWith('**') && p.endsWith('**') && p.length > 4) return <strong key={i}>{inline(p.slice(2, -2))}</strong>
    const a = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(p)
    if (a) {
      const href = a[2]
      return href.startsWith('#')
        ? <a key={i} href={href} onClick={e => { e.preventDefault(); document.getElementById(href.slice(1))?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }}>{inline(a[1])}</a>
        : <a key={i} href={href} target="_blank" rel="noreferrer">{inline(a[1])}</a>
    }
    return <Fragment key={i}>{p}</Fragment>
  })
}
const cells = (l: string) => l.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim())
const isList = (l: string) => /^\s*([-*]|\d+\.)\s+/.test(l)

/** Small, safe markdown renderer: headings, paragraphs, lists, tables, fenced code, inline code, bold and links. */
export default function Markdown({ source }: { source: string }) {
  const lines = source.replace(/<!--[\s\S]*?-->/g, '').split('\n')
  const out: ReactNode[] = []
  let i = 0
  while (i < lines.length) {
    const l = lines[i]
    if (l.startsWith('```')) {
      const buf: string[] = []; i++
      while (i < lines.length && !lines[i].startsWith('```')) buf.push(lines[i++])
      i++; out.push(<pre key={out.length} className="json"><code>{buf.join('\n')}</code></pre>); continue
    }
    const h = /^(#{1,4})\s+(.*)/.exec(l)
    if (h) { const T = `h${h[1].length + 1}` as 'h2' | 'h3' | 'h4' | 'h5'; out.push(<T key={out.length} id={slug(h[2])}>{inline(h[2])}</T>); i++; continue }
    if (l.trim().startsWith('|') && /^\s*\|?\s*:?-{3,}/.test(lines[i + 1] ?? '')) {
      const head = cells(l); i += 2
      const rows: string[][] = []
      while (i < lines.length && lines[i].trim().startsWith('|')) rows.push(cells(lines[i++]))
      out.push(<div key={out.length} className="scroll"><table className="table"><thead><tr>{head.map((c, k) => <th key={k}>{inline(c)}</th>)}</tr></thead>
        <tbody>{rows.map((r, k) => <tr key={k}>{r.map((c, j) => <td key={j}>{inline(c)}</td>)}</tr>)}</tbody></table></div>); continue
    }
    if (isList(l)) {
      const ordered = /^\s*\d+\./.test(l), items: string[] = []
      while (i < lines.length && isList(lines[i])) items.push(lines[i++].replace(/^\s*([-*]|\d+\.)\s+/, ''))
      const L = ordered ? 'ol' : 'ul'
      out.push(<L key={out.length}>{items.map((t, k) => <li key={k}>{inline(t)}</li>)}</L>); continue
    }
    if (!l.trim()) { i++; continue }
    const p: string[] = []
    while (i < lines.length && lines[i].trim() && !lines[i].startsWith('```') && !/^#{1,4}\s/.test(lines[i]) && !isList(lines[i]) && !lines[i].trim().startsWith('|')) p.push(lines[i++])
    out.push(<p key={out.length}>{inline(p.join(' '))}</p>)
  }
  return <div className="md">{out}</div>
}
