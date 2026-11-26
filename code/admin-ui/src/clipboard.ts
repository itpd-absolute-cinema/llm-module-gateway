/** Copies text. The Clipboard API exists only on https and localhost, so on plain http fall back to a hidden textarea. */
export async function copyText(text: string): Promise<void> {
  try { await navigator.clipboard.writeText(text); return } catch { /* try the fallback */ }
  const t = document.createElement('textarea')
  t.value = text; t.setAttribute('readonly', ''); t.style.position = 'fixed'; t.style.top = '0'; t.style.opacity = '0'
  const prev = document.activeElement as HTMLElement | null
  document.body.appendChild(t); t.select(); t.setSelectionRange(0, text.length)
  const ok = document.execCommand('copy')
  t.remove(); prev?.focus?.()
  if (!ok) throw new Error('copy failed')
}
