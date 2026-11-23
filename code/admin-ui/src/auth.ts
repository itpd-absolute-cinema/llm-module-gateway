import { config } from './config'
// Keycloak sign-in: Authorization Code + PKCE for a public client. No extra dependencies.
// The address comes from the runtime config (/config.js), so one image works in every environment.
const KC = { url: config.keycloakUrl, realm: config.keycloakRealm, client: config.keycloakClient }
const base = `${KC.url}/realms/${KC.realm}/protocol/openid-connect`
const redirect = () => location.origin + '/'
interface Tok { access: string; refresh?: string; exp: number }
let tok: Tok | null = null
let expired = false
try { tok = JSON.parse(sessionStorage.getItem('ac-tok') || 'null') } catch { tok = null }

const b64 = (b: ArrayBuffer | Uint8Array) => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
const rnd = () => b64(crypto.getRandomValues(new Uint8Array(32)))

async function tokenReq(params: Record<string, string>) {
  let r: Response
  try { r = await fetch(base + '/token', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ client_id: KC.client, ...params }) }) }
  catch { throw new Error(`Cannot reach Keycloak at ${KC.url}. Most likely the client “${KC.client}” has no Web origins entry for ${location.origin}.`) }
  if (!r.ok) throw new Error('Keycloak rejected the sign-in (' + r.status + ')')
  const j = await r.json()
  expired = false
  tok = { access: j.access_token, refresh: j.refresh_token, exp: Date.now() + j.expires_in * 1000 }
  sessionStorage.setItem('ac-tok', JSON.stringify(tok))
}

export const isAuthed = () => !!tok
export const sessionExpired = () => expired
/** The Keycloak session is over (token expired and could not be refreshed): show the sign-in screen. */
export function lose() {
  if (!tok) return
  tok = null; expired = true; sessionStorage.removeItem('ac-tok')
  dispatchEvent(new Event('ac-auth-lost'))
}
export async function login() {
  if (!KC.url) throw new Error('Keycloak address is not configured. Set KEYCLOAK_PUBLIC_URL for the admin-ui container.')
  const verifier = rnd(), state = rnd()
  sessionStorage.setItem('ac-pkce', JSON.stringify({ verifier, state }))
  const challenge = b64(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier)))
  location.href = `${base}/auth?` + new URLSearchParams({ client_id: KC.client, redirect_uri: redirect(), response_type: 'code', scope: 'openid profile email', code_challenge: challenge, code_challenge_method: 'S256', state })
}
/** Call once before rendering: finishes the redirect from Keycloak if we just came back. */
export async function initAuth(): Promise<string> {
  const p = new URLSearchParams(location.search)
  if (!p.has('code')) return ''
  let err = ''
  try {
    const saved = JSON.parse(sessionStorage.getItem('ac-pkce') || '{}')
    if (saved.state !== p.get('state')) throw new Error('Sign-in state mismatch, please try again')
    await tokenReq({ grant_type: 'authorization_code', code: p.get('code')!, redirect_uri: redirect(), code_verifier: saved.verifier })
  } catch (e) { err = (e as Error).message }
  history.replaceState({}, '', location.pathname)
  return err
}
export async function getToken(): Promise<string> {
  if (tok && tok.exp - Date.now() < 15000) {
    try { if (!tok.refresh) throw new Error('expired'); await tokenReq({ grant_type: 'refresh_token', refresh_token: tok.refresh }) }
    catch { lose() }
  }
  return tok?.access ?? ''
}
export function logout() {
  tok = null; sessionStorage.removeItem('ac-tok')
  location.href = `${base}/logout?` + new URLSearchParams({ client_id: KC.client, post_logout_redirect_uri: redirect() })
}

export interface Claims { name?: string; preferred_username?: string; email?: string; sub?: string; exp?: number; roles: string[] }
/** Reads the user's details from a JWT (the Keycloak session token by default). */
export function claims(raw?: string): Claims | null {
  const t = raw || tok?.access
  if (!t) return null
  try {
    const b = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(atob(b + '='.repeat((4 - (b.length % 4)) % 4)).split('').map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join(''))
    const p = JSON.parse(json)
    return { name: p.name, preferred_username: p.preferred_username, email: p.email, sub: p.sub, exp: p.exp, roles: p.realm_access?.roles ?? [] }
  } catch { return null }
}
