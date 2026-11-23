import { useState } from 'react'
import { motion } from 'framer-motion'
import { login } from '../auth'

export default function Login({ notice }: { notice: string }) {
  const [err, setErr] = useState('')
  return (
    <div className="login">
      <div className="orb o1" /><div className="orb o2" />
      <motion.div className="login-card" initial={{ opacity: 0, y: 24, scale: .98 }} animate={{ opacity: 1, y: 0, scale: 1 }} transition={{ type: 'spring', stiffness: 260, damping: 26 }}>
        <svg width="56" height="56" viewBox="0 0 32 32" aria-hidden><rect width="32" height="32" rx="8" fill="var(--primary)" /><path d="M8 11h16M8 16h10M8 21h13" stroke="var(--on-primary)" strokeWidth="3" strokeLinecap="round" /></svg>
        <h1>AC Gateway</h1>
        <p className="muted">DevOps console</p>
        {(err || notice) && <div className="notice" role="alert">{err || notice}</div>}
        <button className="btn pri wide" onClick={() => login().catch(e => setErr((e as Error).message))} autoFocus>Sign in with Keycloak</button>
      </motion.div>
    </div>
  )
}
