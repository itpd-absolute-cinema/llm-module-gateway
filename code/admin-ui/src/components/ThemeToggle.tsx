import { useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'

export default function ThemeToggle() {
  const [t, setT] = useState(document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light')
  function flip() {
    const n = t === 'dark' ? 'light' : 'dark'
    setT(n); document.documentElement.dataset.theme = n
    try { localStorage.setItem('ac-theme', n) } catch { /* storage unavailable */ }
  }
  const next = t === 'dark' ? 'light' : 'dark'
  return (
    <button className="theme" onClick={flip} aria-label={`Switch to ${next} theme`} title={`Switch to ${next} theme`}>
      <AnimatePresence mode="wait" initial={false}>
        <motion.span key={t} initial={{ rotate: -90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 90, opacity: 0 }} transition={{ duration: .15 }}>
          {t === 'dark' ? '☀' : '☾'}
        </motion.span>
      </AnimatePresence>
    </button>
  )
}
