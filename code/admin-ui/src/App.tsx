import { useEffect, useState } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import Pipeline from './pages/Pipeline'
import NewPlugin from './pages/NewPlugin'
import Models from './pages/Models'
import Statistics from './pages/Statistics'
import Users from './pages/Users'
import Audit from './pages/Audit'
import Login from './components/Login'
import Profile from './pages/Profile'
import Settings from './components/Settings'
import ThemeToggle from './components/ThemeToggle'
import Tour, { startTour, tourDone } from './components/Tour'
import { getCfg } from './api'
import { grafanaUrl } from './grafana'
import { claims, isAuthed, sessionExpired } from './auth'

const nav = [
  { to: '/statistics', t: 'Statistics', i: '▥' },
  { to: '/plugins', t: 'Plugins', i: '⇄' },
  { to: '/models', t: 'Models', i: '◈' },
  { to: '/users', t: 'Users', i: '☺' },
  { to: '/audit', t: 'Audit log', i: '☰' },
]

export default function App({ authError }: { authError: string }) {
  const loc = useLoc()
  const [cfgOpen, setCfgOpen] = useState(false)
  const [, bump] = useState(0)
  useEffect(() => { const h = () => bump(n => n + 1); addEventListener('ac-auth-lost', h); return () => removeEventListener('ac-auth-lost', h) }, [])
  const cfg = getCfg()
  const cur = [...nav, { to: '/profile', t: 'Profile' }].find(n => loc.pathname.startsWith(n.to))
  const me = claims(cfg.token || undefined)
  const myName = me?.name || me?.preferred_username || (cfg.demo ? 'Demo user' : 'Profile')
  const signedIn = cfg.demo || !!cfg.token || isAuthed()
  const [tourOpen, setTourOpen] = useState(false)
  const tourUser = me?.sub || me?.preferred_username || 'local'
  // first visit: start the onboarding once; afterwards it can be restarted from the menu or the profile
  useEffect(() => {
    if (!signedIn) return
    const h = () => setTourOpen(true)
    addEventListener('ac-tour-start', h)
    const t = tourDone(tourUser) ? 0 : setTimeout(h, 700)
    return () => { removeEventListener('ac-tour-start', h); if (t) clearTimeout(t) }
  }, [signedIn, tourUser])

  if (!signedIn) return <Login notice={sessionExpired() ? 'Your session has expired. Please sign in again.' : authError} />
  return (
    <>
      <aside className="side">
        <div className="brand"><span className="mark" />AC-GATEWAY<small>DevOps console</small></div>
        <nav aria-label="Sections">
          {nav.map(n => (
            <NavLink key={n.to} to={n.to} className="nav" data-tour={'nav-' + n.to.slice(1)}>
              {({ isActive }) => (<>
                {isActive && <motion.span layoutId="nav-bg" className="nav-bg" transition={{ type: 'spring', stiffness: 500, damping: 40 }} />}
                <i>{n.i}</i><span>{n.t}</span>
              </>)}
            </NavLink>
          ))}
          <a className="nav" data-tour="nav-grafana" href={grafanaUrl()} target="_blank" rel="noreferrer"><i>◔</i><span>Grafana</span><em>↗</em></a>
          <button type="button" className="nav" data-tour="nav-tour" onClick={startTour}><i>?</i><span>Onboarding</span></button>
        </nav>
        <NavLink to="/profile" data-tour="profile" className={({ isActive }) => 'chip' + (isActive ? ' active' : '')}>
          <span className="avatar">{myName[0].toUpperCase()}</span><span>{myName}</span>
        </NavLink>
      </aside>
      <div className="shell">
        <header className="top">
          <span>Gateway <span className="sep">/</span> {loc.pathname === '/plugins/new' ? <><NavLink to="/plugins">Plugins</NavLink> <span className="sep">/</span> <strong>New plugin</strong></> : <strong>{cur?.t}</strong>}</span>
          <div className="tools" data-tour="top-tools">
            <ThemeToggle />
            <button className={'mode' + (cfg.demo ? '' : ' live')} onClick={() => setCfgOpen(true)}><b />{cfg.demo ? 'Demo data · settings' : `Connected: ${cfg.url}`}</button>
          </div>
        </header>
        <AnimatePresence mode="wait">
          <motion.main key={loc.pathname} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }} transition={{ duration: .22 }}>
            <Routes location={loc}>
              <Route path="/" element={<Navigate to="/statistics" replace />} />
              <Route path="/plugins" element={<Pipeline />} />
              <Route path="/plugins/new" element={<NewPlugin />} />
              <Route path="/pipeline" element={<Navigate to="/plugins" replace />} />
              <Route path="/models" element={<Models />} />
              <Route path="/statistics" element={<Statistics />} />
              <Route path="/users" element={<Users />} />
              <Route path="/audit" element={<Audit />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="*" element={<Navigate to="/statistics" replace />} />
            </Routes>
          </motion.main>
        </AnimatePresence>
      </div>
      <Settings open={cfgOpen} onClose={() => setCfgOpen(false)} />
      <Tour open={tourOpen} user={tourUser} onClose={() => setTourOpen(false)} />
    </>
  )
}
const useLoc = useLocation
