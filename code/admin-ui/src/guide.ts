/** The plugin guide is a plain markdown file served next to the app, so it can be replaced without a rebuild. */
export async function fetchGuide(): Promise<string> {
  const r = await fetch(import.meta.env.BASE_URL + 'PLUGINS.md', { cache: 'no-store' })
  const t = r.ok ? await r.text() : ''
  if (!t.trim() || /^\s*<(!doctype|html)/i.test(t)) throw new Error('PLUGINS.md was not found next to the app')
  return t
}
export const buildPrompt = (md: string, task: string) => task.trim() ? `${md.trimEnd()}\n\n## Task\n${task.trim()}\n` : md
export async function copyPrompt(task = ''): Promise<void> {
  await navigator.clipboard.writeText(buildPrompt(await fetchGuide(), task))
}
