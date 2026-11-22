import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, type MotionStyle } from 'framer-motion'
import { useLocation, useNavigate } from 'react-router-dom'
import { CHAPTERS, STEPS } from '../tourSteps'

const key = (user: string) => `ac-tour-done:${user}`
/** Dispatch from anywhere (sidebar, profile) to start the tour from the beginning. */
export const startTour = () => dispatchEvent(new Event('ac-tour-start'))
/** Without working storage we cannot remember completion, so we never auto-start. */
export const tourDone = (user: string) => { try { return !!localStorage.getItem(key(user)) } catch { return true } }
const markDone = (user: string) => { try { localStorage.setItem(key(user), '1') } catch { /* storage unavailable */ } }

type Box = { top: number; left: number; width: number; height: number }
const PAD = 6

export default function Tour({ open, user, onClose }: { open: boolean; user: string; onClose: () => void }) {
  const nav = useNavigate(), loc = useLocation()
  const [i, setI] = useState(0)
  const [box, setBox] = useState<Box | null>(null)
  const [narrow, setNarrow] = useState(() => innerWidth < 900)
  const card = useRef<HTMLDivElement>(null)
  const step = STEPS[i], last = i === STEPS.length - 1

  const finish = useCallback(() => { markDone(user); onClose() }, [user, onClose])
  const go = useCallback((n: number) => setI(Math.max(0, Math.min(STEPS.length - 1, n))), [])

  useEffect(() => { if (open) setI(0) }, [open])
  // open the page this step talks about
  useEffect(() => { if (open && step.path && loc.pathname !== step.path) nav(step.path) }, [open, i]) // eslint-disable-line react-hooks/exhaustive-deps

  // find and follow the highlighted element (pages animate in and load data, so we keep measuring)
  useEffect(() => {
    if (!open) return
    setBox(null)
    let scrolled = false
    const measure = () => {
      setNarrow(innerWidth < 900)
      const el = step.target ? document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`) : null
      if (!el) return setBox(b => (b ? null : b))
      if (!scrolled) { scrolled = true; el.scrollIntoView({ block: 'nearest', behavior: 'smooth' }) }
      const r = el.getBoundingClientRect()
      setBox(b => b && Math.abs(b.top - r.top) < .5 && Math.abs(b.left - r.left) < .5 && Math.abs(b.width - r.width) < .5 && Math.abs(b.height - r.height) < .5
        ? b : { top: r.top, left: r.left, width: r.width, height: r.height })
    }
    measure()
    const t = setInterval(measure, 150)
    addEventListener('resize', measure)
    return () => { clearInterval(t); removeEventListener('resize', measure) }
  }, [open, i, loc.pathname]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!open) return
    const h = (e: KeyboardEvent) => {
      if (e.key === 'Escape') finish()
      else if (e.key === 'ArrowRight') last ? finish() : go(i + 1)
      else if (e.key === 'ArrowLeft') go(i - 1)
    }
    addEventListener('keydown', h); return () => removeEventListener('keydown', h)
  }, [open, i, last, finish, go])

  useLayoutEffect(() => { if (open) { card.current?.focus(); card.current?.scrollTo({ top: 0 }) } }, [open, i])

  if (!open) return null
  const w = step.wide ? 540 : 400
  let pos: MotionStyle
  if (narrow) pos = { left: 12, right: 12, bottom: 12, width: 'auto' }
  else if (!box) pos = { left: '50%', top: '50%', transform: 'translate(-50%,-50%)', width: w }
  else if (box.left < 300 && box.left + box.width + 24 + w < innerWidth) pos = { left: box.left + box.width + 24, top: Math.max(16, Math.min(box.top, innerHeight - 420)), width: w }
  else if (innerHeight - (box.top + box.height) > 340) pos = { left: Math.max(16, Math.min(box.left, innerWidth - w - 16)), top: box.top + box.height + 16, width: w }
  else if (box.top > 340 && box.height < innerHeight * .5) pos = { left: Math.max(16, Math.min(box.left, innerWidth - w - 16)), bottom: innerHeight - box.top + 16, width: w }
  else pos = { right: 24, bottom: 24, width: w }

  const chIdx = CHAPTERS.indexOf(step.ch)
  return (
    <div className="tour" role="presentation">
      <div className="tour-block" onMouseDown={e => e.stopPropagation()} />
      {box
        ? <div className="tour-spot" style={{ top: box.top - PAD, left: box.left - PAD, width: box.width + PAD * 2, height: box.height + PAD * 2 }} />
        : <div className="tour-dim" />}
      <AnimatePresence mode="wait">
        <motion.div key={step.id} ref={card} tabIndex={-1} className="tour-card" role="dialog" aria-modal="true" aria-label={`Onboarding: ${step.title}`}
          style={pos} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: .16 }}>
          <div className="tour-chapters" role="tablist" aria-label="Chapters">
            {CHAPTERS.map((c, n) => (
              <button key={c} type="button" role="tab" aria-selected={n === chIdx} className={n === chIdx ? 'on' : ''}
                onClick={() => go(STEPS.findIndex(s => s.ch === c))}>{c}</button>
            ))}
            <span className="tour-count">{i + 1} / {STEPS.length}</span>
          </div>
          <div className="tour-bar" aria-hidden><span style={{ width: `${((i + 1) / STEPS.length) * 100}%` }} /></div>
          <h2 className="tour-title">{step.title}</h2>
          <div className="tour-body">{step.body}</div>
          <div className="tour-foot">
            <button type="button" className="link" onClick={finish}>{last ? 'Close' : 'Skip tour'}</button>
            <div className="acts">
              <button type="button" className="btn" disabled={i === 0} onClick={() => go(i - 1)}>Back</button>
              <button type="button" className="btn pri" onClick={() => (last ? finish() : go(i + 1))}>{last ? 'Finish' : i === 0 ? 'Start tour' : 'Next'}</button>
            </div>
          </div>
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
