export const num = (n?: number | null) => n == null ? '—' : new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 }).format(n)
export const int = (n?: number | null) => n == null ? '—' : n.toLocaleString('en')
export const money = (n?: number | null, c = 'USD') => n == null ? '—' : new Intl.NumberFormat('en', { style: 'currency', currency: c, maximumFractionDigits: n !== 0 && Math.abs(n) < 1 ? 4 : 2 }).format(n)
export const when = (s?: string | null) => s ? new Date(s).toLocaleString('en', { dateStyle: 'medium', timeStyle: 'short' }) : '—'
