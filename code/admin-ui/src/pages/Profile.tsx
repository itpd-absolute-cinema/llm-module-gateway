import { Fragment } from 'react'
import { claims, isAuthed, logout } from '../auth'
import { getCfg, saveCfg } from '../api'
import { when } from '../fmt'
import { startTour } from '../components/Tour'

export default function Profile() {
  const cfg = getCfg()
  const manual = !!cfg.token
  const c = claims(manual ? cfg.token : undefined)
  const name = c?.name || c?.preferred_username || (cfg.demo ? 'Demo user' : 'Unknown user')
  const via = cfg.demo ? 'Demo data (no gateway)' : manual ? 'Manual token' : 'Keycloak'
  const rows: [string, React.ReactNode][] = [
    ['Username', c?.preferred_username || '—'],
    ['Email', c?.email || '—'],
    ['User id', c?.sub ? <span className="mono">{c.sub}</span> : '—'],
    ['Roles', c?.roles.length ? c.roles.map(r => <span key={r} className="pill2">{r}</span>) : '—'],
    ['Session expires', c?.exp ? when(new Date(c.exp * 1000).toISOString()) : '—'],
    ['Signed in via', via],
    ['Gateway', cfg.demo ? '—' : cfg.url],
  ]
  function leave() {
    if (isAuthed() && !manual) return logout()
    saveCfg({ ...cfg, token: '', demo: false }); location.href = '/'
  }
  return (
    <div className="profile">
      <h1>Profile</h1>
      <p className="lead">Your account and current session.</p>
      <div className="card">
        <div className="who"><span className="avatar big">{name[0].toUpperCase()}</span><div><h2>{name}</h2><span className="muted">{c?.email || via}</span></div></div>
        <dl className="kv">{rows.map(([k, v]) => <Fragment key={k}><dt>{k}</dt><dd>{v}</dd></Fragment>)}</dl>
        <div className="acts">
          <button className="btn pri" onClick={leave}>{manual ? 'Remove token and sign out' : cfg.demo ? 'Leave demo mode' : 'Sign out'}</button>
          <button className="btn" onClick={startTour}>Restart onboarding</button>
        </div>
      </div>
    </div>
  )
}
