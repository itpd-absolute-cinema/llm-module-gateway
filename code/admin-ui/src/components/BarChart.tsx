import { motion } from 'framer-motion'

export default function BarChart({ data, format }: { data: { label: string; value: number }[]; format: (n: number) => string }) {
  if (!data.length) return <div className="state">No data for this period</div>
  const w = 680, h = 190, pad = 8, max = Math.max(...data.map(d => d.value), 1)
  const bw = (w - pad * 2) / data.length
  const ticks = [0, Math.floor((data.length - 1) / 2), data.length - 1]
  return (
    <svg viewBox={`0 0 ${w} ${h + 24}`} className="chart" role="img" aria-label="Usage over time">
      {[0, .5, 1].map(t => <line key={t} className="grid" x1={pad} x2={w - pad} y1={h - t * (h - 12)} y2={h - t * (h - 12)} />)}
      {data.map((d, i) => {
        const bh = (d.value / max) * (h - 12)
        return (
          <motion.rect key={d.label} className="bar" x={pad + i * bw + bw * .15} width={bw * .7} rx={3}
            initial={{ y: h, height: 0 }} animate={{ y: h - bh, height: bh }} transition={{ delay: Math.min(i * .015, .5), duration: .4 }}>
            <title>{`${d.label}: ${format(d.value)}`}</title>
          </motion.rect>
        )
      })}
      {[...new Set(ticks)].map(i => <text key={i} x={pad + i * bw + bw / 2} y={h + 17} textAnchor={i === 0 ? 'start' : i === data.length - 1 ? 'end' : 'middle'}>{data[i].label}</text>)}
    </svg>
  )
}
