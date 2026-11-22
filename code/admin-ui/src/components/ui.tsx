import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'

type Notify = (text: string, bad?: boolean) => void
const Ctx = createContext<Notify>(() => {})
export const useToast = () => useContext(Ctx)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<{ id: number; text: string; bad: boolean }[]>([])
  const notify = useCallback<Notify>((text, bad = false) => {
    const id = Date.now() + Math.random()
    setItems(l => [...l, { id, text, bad }])
    setTimeout(() => setItems(l => l.filter(i => i.id !== id)), 3500)
  }, [])
  return (
    <Ctx.Provider value={notify}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        <AnimatePresence>
          {items.map(i => (
            <motion.div key={i.id} className={'toast' + (i.bad ? ' bad' : '')} layout
              initial={{ opacity: 0, y: 24, scale: .96 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, x: 40 }}>
              {i.text}
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </Ctx.Provider>
  )
}

export function Modal({ open, onClose, children, wide = false, xl = false }: { open: boolean; onClose: () => void; children: ReactNode; wide?: boolean; xl?: boolean }) {
  useEffect(() => {
    if (!open) return
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    addEventListener('keydown', h); return () => removeEventListener('keydown', h)
  }, [open, onClose])
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onMouseDown={onClose}>
          <motion.div className={'modal' + (xl ? ' xl' : wide ? ' wide' : '')} role="dialog" aria-modal="true" onMouseDown={e => e.stopPropagation()}
            initial={{ opacity: 0, y: 18, scale: .97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 10, scale: .98 }}
            transition={{ type: 'spring', stiffness: 420, damping: 32 }}>
            {children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

/** Smooth count-up for stat cards */
export function CountUp({ to }: { to: number }) {
  const [v, setV] = useState(0)
  useEffect(() => {
    const t0 = performance.now(); let raf = 0
    const tick = (t: number) => { const k = Math.min(1, (t - t0) / 700); setV(Math.round(to * (1 - Math.pow(1 - k, 3)))); if (k < 1) raf = requestAnimationFrame(tick) }
    raf = requestAnimationFrame(tick); return () => cancelAnimationFrame(raf)
  }, [to])
  return <>{v}</>
}

/** Load data on mount and whenever deps change; errors are returned, not thrown. */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const reload = useCallback(() => {
    setLoading(true); setErr('')
    fn().then(setData).catch(e => setErr((e as Error).message)).finally(() => setLoading(false))
  }, deps) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(reload, [reload])
  return { data, loading, err, reload }
}

export function Drawer({ open, onClose, children }: { open: boolean; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    if (!open) return
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    addEventListener('keydown', h); return () => removeEventListener('keydown', h)
  }, [open, onClose])
  return (
    <AnimatePresence>
      {open && (<>
        <motion.div className="scrim" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose} />
        <motion.aside className="drawer" initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }} transition={{ type: 'spring', stiffness: 380, damping: 38 }}>{children}</motion.aside>
      </>)}
    </AnimatePresence>
  )
}

export function Pager({ page, size, total, onPage }: { page: number; size: number; total: number; onPage: (p: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <div className="pager">
      <span>{total.toLocaleString('en')} total · page {page + 1} of {pages}</span>
      <button className="btn" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</button>
      <button className="btn" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  )
}
