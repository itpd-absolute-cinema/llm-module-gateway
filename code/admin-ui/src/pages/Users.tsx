import { FormEvent, useEffect, useState } from 'react'
import { blockUser, createUser, listUsers, unblockUser, usageSummary } from '../api'
import type { UserView } from '../types'
import { Drawer, Modal, Pager, useLoad, useToast } from '../components/ui'
import { int, money, num, when } from '../fmt'
import OpenCodeConfig from '../components/OpenCodeConfig'
import OpenCodeGuide from '../components/OpenCodeGuide'

function status(u: UserView) {
  if (u.blocked) return <span className="pill2 bad">Blocked</span>
  if (u.enabled === false) return <span className="pill2 mute">Disabled in Keycloak</span>
  return <span className="pill2">Active</span>
}

export default function Users() {
  const toast = useToast()
  const [q, setQ] = useState(''), [search, setSearch] = useState(''), [page, setPage] = useState(0)
  const [target, setTarget] = useState<UserView | null>(null), [reason, setReason] = useState(''), [busy, setBusy] = useState(false)
  const [open, setOpen] = useState<UserView | null>(null)
  const [create, setCreate] = useState(false)
  const [tab, setTab] = useState<'users' | 'setup'>('users'), [ocOpen, setOcOpen] = useState(false), [guideOpen, setGuideOpen] = useState(false)
  const d = useLoad(() => listUsers({ search, page, size: 15 }), [search, page])

  async function block(e: FormEvent) {
    e.preventDefault(); if (!target) return
    if (!reason.trim()) return toast('Please give a reason', true)
    setBusy(true)
    try { await blockUser(target.id, reason.trim()); toast('User blocked. The next request gets 403'); setTarget(null); setReason(''); d.reload() }
    catch (x) { toast((x as Error).message, true) }
    setBusy(false)
  }
  async function unblock(u: UserView) {
    try { await unblockUser(u.id); toast('User unblocked'); setOpen(null); d.reload() } catch (x) { toast((x as Error).message, true) }
  }
  return (
    <>
      <div className="head"><div><h1>Users</h1><p className="lead">People and services calling the gateway. Blocking takes effect on their next request.</p></div>
        {tab === 'users' && <button className="btn pri" onClick={() => setCreate(true)}>Create user</button>}</div>
      <div className="seg mb" data-tour="users-tabs" role="tablist" aria-label="Users sections">
        <button type="button" role="tab" aria-selected={tab === 'users'} className={tab === 'users' ? 'on' : ''} onClick={() => setTab('users')}>Users</button>
        <button type="button" role="tab" aria-selected={tab === 'setup'} className={tab === 'setup' ? 'on' : ''} onClick={() => setTab('setup')}>Setup</button>
      </div>
      {tab === 'setup' && (
        <div className="card">
          <h2>Connect OpenCode</h2>
          <p className="muted">Generate the OpenCode configuration for signing in through Keycloak and using the gateway models.</p>
          <div className="acts">
            <button className="btn pri" onClick={() => setOcOpen(true)}>OpenCode config</button>
            <button className="btn" onClick={() => setGuideOpen(true)}>Setup instructions</button>
          </div>
        </div>
      )}
      {tab === 'users' && (<>
      <form className="filters" onSubmit={e => { e.preventDefault(); setPage(0); setSearch(q.trim()) }}>
        <div><label htmlFor="s">Search</label><input id="s" value={q} onChange={e => setQ(e.target.value)} placeholder="Username or email" /></div>
        <button className="btn pri">Search</button>
      </form>
      {d.err && <div className="card state danger-text mb">{d.err}</div>}
      <div className="card scroll">
        {d.loading && !d.data ? <div className="skel block" /> : (
          <table className="table"><thead><tr><th>User</th><th>Status</th><th>Last activity</th><th /></tr></thead>
            <tbody>{d.data?.items.map(u => (
              <tr key={u.id} className="click" onClick={() => setOpen(u)}>
                <td><b>{u.username || u.id}</b><div className="muted">{u.email || ''}</div></td><td>{status(u)}</td><td>{when(u.last_activity)}</td>
                <td onClick={e => e.stopPropagation()}>{u.blocked
                  ? <button className="btn" onClick={() => unblock(u)}>Unblock</button>
                  : <button className="btn danger-text" onClick={() => { setTarget(u); setReason('') }}>Block</button>}</td></tr>
            ))}</tbody></table>
        )}
        {d.data && !d.data.items.length && <div className="state">No users found</div>}
        {d.data && <Pager page={d.data.page} size={d.data.size} total={d.data.total} onPage={setPage} />}
      </div>
      </>)}

      <OpenCodeConfig open={ocOpen} onClose={() => setOcOpen(false)} />
      <OpenCodeGuide open={guideOpen} onClose={() => setGuideOpen(false)} />
      <CreateUserModal open={create} onClose={() => setCreate(false)} onDone={() => { setCreate(false); toast('User created with the chat role'); d.reload() }} />
      <Modal open={!!target} onClose={() => setTarget(null)}>
        <form onSubmit={block}>
          <h2>Block {target?.username || target?.id}</h2>
          <p className="muted">The gateway rejects their requests right away. If configured, the Keycloak account is disabled too.</p>
          <label htmlFor="r">Reason</label><textarea id="r" style={{ minHeight: 90 }} maxLength={500} value={reason} onChange={e => setReason(e.target.value)} />
          <div className="row"><button type="button" className="btn" onClick={() => setTarget(null)}>Cancel</button><button className="btn pri" disabled={busy}>{busy ? 'Blocking…' : 'Block user'}</button></div>
        </form>
      </Modal>
      <Drawer open={!!open} onClose={() => setOpen(null)}>{open && <UserPanel u={open} onUnblock={unblock} onBlock={u => { setOpen(null); setTarget(u); setReason('') }} />}</Drawer>
    </>
  )
}

function UserPanel({ u, onUnblock, onBlock }: { u: UserView; onUnblock: (u: UserView) => void; onBlock: (u: UserView) => void }) {
  const s = useLoad(() => usageSummary({ user_id: u.id }), [u.id])
  const rows: [string, string][] = [
    ['Requests, 30 days', int(s.data?.requests)], ['Tokens', num(s.data?.total_tokens)], ['Spend', money(s.data?.cost, s.data?.currency || 'USD')],
    ['Error rate', s.data ? (s.data.error_rate * 100).toFixed(1) + '%' : '—'],
  ]
  return (<>
    <h2>{u.username || u.id}</h2><div className="muted">{u.email}</div><div>{status(u)}</div>
    <label>User id</label><div className="mono">{u.id}</div>
    <label>Last activity</label><div>{when(u.last_activity)}</div>
    {u.blocked && u.block && (<><label>Block</label>
      <div>{u.block.reason}</div><div className="muted">{u.block.blocked_by} · {when(u.block.blocked_at)}{u.block.keycloak_disabled ? ' · Keycloak account disabled' : ''}</div></>)}
    <label>Usage (last 30 days)</label>
    {s.err ? <div className="danger-text">{s.err}</div> : <table className="table"><tbody>{rows.map(([k, v]) => <tr key={k}><td className="muted">{k}</td><td>{s.loading ? '…' : v}</td></tr>)}</tbody></table>}
    <div className="row">{u.blocked ? <button className="btn pri" onClick={() => onUnblock(u)}>Unblock</button> : <button className="btn danger-text" onClick={() => onBlock(u)}>Block user</button>}</div>
  </>)
}

const genPassword = () => { const a = crypto.getRandomValues(new Uint32Array(16)); return Array.from(a, n => 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'[n % 55]).join('') }

function CreateUserModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const empty = { username: '', email: '', first_name: '', last_name: '', password: '', temporary_password: false }
  const [f, setF] = useState(empty), [err, setErr] = useState(''), [busy, setBusy] = useState(false)
  useEffect(() => { if (open) { setF(empty); setErr('') } }, [open]) // eslint-disable-line react-hooks/exhaustive-deps
  const set = (k: keyof typeof empty) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value })
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!f.username.trim() || !f.password) return setErr('Username and password are required')
    setBusy(true); setErr('')
    try {
      await createUser({ ...f, username: f.username.trim(), email: f.email.trim() || undefined, first_name: f.first_name.trim() || undefined, last_name: f.last_name.trim() || undefined })
      onDone()
    } catch (x) { setErr((x as Error).message) }
    setBusy(false)
  }
  return (
    <Modal open={open} onClose={onClose}>
      <form onSubmit={submit}>
        <h2>Create user</h2>
        <p className="muted">The new account gets the chat-user role and can call /chat/completions.</p>
        <label htmlFor="cu">Username</label><input id="cu" value={f.username} onChange={set('username')} autoComplete="off" />
        <label htmlFor="ce">Email (optional)</label><input id="ce" type="email" value={f.email} onChange={set('email')} />
        <div className="cols">
          <div><label htmlFor="cf">First name</label><input id="cf" value={f.first_name} onChange={set('first_name')} /></div>
          <div><label htmlFor="cl">Last name</label><input id="cl" value={f.last_name} onChange={set('last_name')} /></div>
        </div>
        <label htmlFor="cp">Password</label>
        <div className="acts"><input id="cp" style={{ flex: 1 }} value={f.password} onChange={set('password')} autoComplete="off" />
          <button type="button" className="btn" onClick={() => setF({ ...f, password: genPassword() })}>Generate</button></div>
        <label className="check"><input type="checkbox" checked={f.temporary_password} onChange={e => setF({ ...f, temporary_password: e.target.checked })} /> User must change the password at first sign-in</label>
        <div className="err">{err}</div>
        <div className="row"><button type="button" className="btn" onClick={onClose}>Cancel</button><button className="btn pri" disabled={busy}>{busy ? 'Creating…' : 'Create user'}</button></div>
      </form>
    </Modal>
  )
}
