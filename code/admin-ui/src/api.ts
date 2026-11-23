import type { AdminModel, AuditEntry, Page, Plugin, PluginType, PriceItem, PricingTable, Status, Timeseries, UsageEvent, UsageSummary, UserView } from './types'
import { getToken, lose } from './auth'
import { config } from './config'

export interface Cfg { url: string; token: string; demo: boolean }
const KEY = 'gw-admin-cfg'

export function getCfg(): Cfg {
  const d: Cfg = { url: config.apiUrl, token: '', demo: false }
  try { return { ...d, ...JSON.parse(localStorage.getItem(KEY) || '{}') } } catch { return d }
}
export function saveCfg(c: Cfg) { try { localStorage.setItem(KEY, JSON.stringify(c)) } catch { /* storage unavailable */ } }

async function http<T>(m: string, path: string, body?: unknown, form?: FormData): Promise<T> {
  const c = getCfg()
  const h: Record<string, string> = {}
  const tk = c.token || await getToken()
  if (tk) h.Authorization = `Bearer ${tk}`
  if (body) h['Content-Type'] = 'application/json'
  let r: Response
  try { r = await fetch(c.url.replace(/\/$/, '') + path, { method: m, headers: h, body: form ?? (body ? JSON.stringify(body) : undefined) }) }
  catch { throw new Error('Gateway is unreachable. Check the address in settings.') }
  if (r.status === 401 && !c.token) lose()
  if (!r.ok) {
    let t = r.status === 401 ? 'Admin token required' : r.status === 403 ? 'Insufficient permissions' : `Error ${r.status}`
    try { t = (await r.json())?.error?.message ?? t } catch { /* body is not JSON */ }
    throw new Error(t)
  }
  if (r.status === 204) return undefined as T
  return (await r.json().catch(() => undefined)) as T
}

/** New list order. before and after are mutually exclusive, as in the gateway API. */
export function place<T extends { name: string }>(list: T[], name: string, before?: string, after?: string): T[] {
  const item = list.find(p => p.name === name)
  if (!item || (!before && !after)) return list
  const rest = list.filter(p => p !== item)
  let at = before ? rest.findIndex(p => p.name === before) : -1
  if (at < 0 && after) { const i = rest.findIndex(p => p.name === after); at = i < 0 ? -1 : i + 1 }
  if (at < 0) return list
  return [...rest.slice(0, at), item, ...rest.slice(at)]
}

/* ---- demo data for running without a gateway ---- */
let db: Plugin[] = [
  { name: 'pii_masker', type: 'request_filter', enabled: true, options: { mask: '***', fields: ['email', 'phone'] } },
  { name: 'prompt_guard', type: 'request_filter', enabled: true, options: { max_chars: 8000 } },
  { name: 'language_router', type: 'request_filter', enabled: false, options: {} },
  { name: 'profanity_filter', type: 'response_filter', enabled: true, options: { level: 'strict' } },
  { name: 'json_repair', type: 'response_filter', enabled: false, options: {} },
]
const wait = <T,>(v: T) => new Promise<T>(r => setTimeout(() => r(v), 180))
const enc = encodeURIComponent
const url = (n: string) => `/admin/plugins/${enc(n)}`

/** Needs GET and PUT /admin/plugins/{name}/source in the gateway (JSON body { source }). */
export const getPluginSource = (name: string): Promise<string> =>
  getCfg().demo ? wait('# Demo mode: the source of a real plugin appears here\n') : http<{ source: string }>('GET', `${url(name)}/source`).then(r => r.source)
export const setPluginSource = (name: string, source: string): Promise<void> =>
  getCfg().demo ? wait(undefined) : http<void>('PUT', `${url(name)}/source`, { source })

export const listPlugins = (): Promise<Plugin[]> =>
  getCfg().demo ? wait(db.map(p => ({ ...p }))) : http('GET', '/admin/plugins')

export async function setStatus(name: string, s: Status): Promise<void> {
  if (!getCfg().demo) return http('PATCH', url(name), s)
  const p = db.find(x => x.name === name); if (!p) throw new Error('Plugin not found')
  if (s.enabled !== undefined) p.enabled = s.enabled
  db = place(db, name, s.before, s.after); await wait(0)
}
export async function setOptions(name: string, options: Record<string, unknown>): Promise<void> {
  if (!getCfg().demo) return http('PUT', url(name), { options })
  const p = db.find(x => x.name === name); if (!p) throw new Error('Plugin not found')
  p.options = options; await wait(0)
}
export async function removePlugin(name: string): Promise<void> {
  if (!getCfg().demo) return http('DELETE', url(name))
  const p = db.find(x => x.name === name)
  if (p?.enabled) throw new Error('Disable the plugin first')
  db = db.filter(x => x !== p); await wait(0)
}
export async function uploadPlugin(file: File, type: PluginType, options: string): Promise<void> {
  const fd = new FormData(); fd.append('file', file)
  if (!getCfg().demo) return http('POST', `/admin/plugins?type=${type}&options=${enc(options)}`, undefined, fd)
  const name = file.name.replace(/\.py$/, '')
  if (db.some(p => p.name === name)) throw new Error(`Plugin “${name}” already exists`)
  db = [...db, { name, type, enabled: false, options: JSON.parse(options) }]; await wait(0)
}

/* ---- endpoints that need a real gateway ---- */
const qs = (o: Record<string, string | number | undefined>) => {
  const p = new URLSearchParams()
  for (const [k, v] of Object.entries(o)) if (v !== undefined && v !== '') p.set(k, String(v))
  const t = p.toString(); return t ? '?' + t : ''
}
const live = <T,>(m: string, path: string, body?: unknown): Promise<T> =>
  getCfg().demo ? Promise.reject(new Error('This page needs a connected gateway. Turn off demo data in settings.')) : http<T>(m, path, body)

type F = { from?: string; to?: string; user_id?: string; model?: string }
export const listModels = () => live<{ items: AdminModel[] }>('GET', '/admin/models')
export const getPricing = () => live<PricingTable>('GET', '/admin/pricing')
export const putPricing = (items: PriceItem[]) =>
  live<PricingTable>('PUT', '/admin/pricing', { items: items.map(({ model, input_price_per_1m, output_price_per_1m, currency }) => ({ model, input_price_per_1m, output_price_per_1m, currency })) })
export const listUsers = (p: { search?: string; page?: number; size?: number }) => live<Page<UserView>>('GET', '/admin/users' + qs(p))
export interface NewUser { username: string; email?: string; first_name?: string; last_name?: string; password: string; temporary_password: boolean }
/** Needs POST /admin/users in the gateway: creates the Keycloak user and grants the chat-user role. */
export const createUser = (u: NewUser) => live<UserView>('POST', '/admin/users', u)
export const blockUser = (id: string, reason: string) => live<UserView>('POST', `/admin/users/${enc(id)}/block`, { reason })
export const unblockUser = (id: string) => live<UserView>('POST', `/admin/users/${enc(id)}/unblock`)
export const usageSummary = (f: F) => live<UsageSummary>('GET', '/admin/usage/summary' + qs(f))
export const usageSeries = (f: F & { interval?: string; group_by?: string }) => live<Timeseries>('GET', '/admin/usage/timeseries' + qs(f))
export const usageEvents = (f: F & { status?: string; page?: number; size?: number }) => live<Page<UsageEvent>>('GET', '/admin/usage/events' + qs(f))
export const auditList = (f: { from?: string; to?: string; actor?: string; action?: string; page?: number; size?: number }) => live<Page<AuditEntry>>('GET', '/admin/audit' + qs(f))

export async function downloadCsv(f: F & { status?: string }): Promise<void> {
  const c = getCfg()
  if (c.demo) throw new Error('Export needs a connected gateway.')
  const tk = c.token || await getToken()
  const r = await fetch(c.url.replace(/\/$/, '') + '/admin/usage/export.csv' + qs(f), { headers: tk ? { Authorization: `Bearer ${tk}` } : {} })
  if (!r.ok) throw new Error(`Export failed (${r.status})`)
  const a = document.createElement('a')
  a.href = URL.createObjectURL(await r.blob()); a.download = 'ac-gateway-usage.csv'; a.click(); URL.revokeObjectURL(a.href)
}
