import { Fragment, useState } from 'react'
import { auditList } from '../api'
import { Pager, useLoad } from '../components/ui'
import { when } from '../fmt'

export default function Audit() {
  const [f, setF] = useState({ actor: '', action: '' }), [draft, setDraft] = useState({ actor: '', action: '' })
  const [page, setPage] = useState(0), [open, setOpen] = useState<string | null>(null)
  const d = useLoad(() => auditList({ actor: f.actor, action: f.action, page, size: 20 }), [f, page])
  return (
    <>
      <h1>Audit log</h1><p className="lead">Who changed plugins, users and prices, and what the change was.</p>
      <form className="filters" onSubmit={e => { e.preventDefault(); setPage(0); setF({ actor: draft.actor.trim(), action: draft.action.trim() }) }}>
        <div><label htmlFor="a">Actor</label><input id="a" value={draft.actor} onChange={e => setDraft({ ...draft, actor: e.target.value })} placeholder="Admin id or username" /></div>
        <div><label htmlFor="b">Action</label><input id="b" value={draft.action} onChange={e => setDraft({ ...draft, action: e.target.value })} placeholder="Any action" /></div>
        <button className="btn pri">Apply</button>
      </form>
      {d.err && <div className="card state danger-text mb">{d.err}</div>}
      <div className="card scroll">
        {d.loading && !d.data ? <div className="skel block" /> : (
          <table className="table"><thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Target</th></tr></thead>
            <tbody>{d.data?.items.map((e, i) => {
              const k = `${e.timestamp}-${i}`
              return (<Fragment key={k}>
                <tr className="click" onClick={() => setOpen(open === k ? null : k)}>
                  <td>{when(e.timestamp)}</td><td>{e.actor_username || e.actor}</td><td><b>{e.action}</b></td><td>{[e.target_type, e.target_id].filter(Boolean).join(' · ') || '—'}</td></tr>
                {open === k && <tr><td colSpan={4}><div className="cols">
                  <div><label>Before</label><pre className="json">{JSON.stringify(e.before ?? null, null, 2)}</pre></div>
                  <div><label>After</label><pre className="json">{JSON.stringify(e.after ?? null, null, 2)}</pre></div></div></td></tr>}
              </Fragment>)
            })}</tbody></table>
        )}
        {d.data && !d.data.items.length && <div className="state">No entries</div>}
        {d.data && <Pager page={d.data.page} size={d.data.size} total={d.data.total} onPage={setPage} />}
      </div>
    </>
  )
}
