import { useEffect, useMemo, useState } from 'react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const blank = { routeId: '', path: '', uri: '', stripPrefix: 0, rateLimitPerMin: 100, enabled: true }

async function call(path, token, options = {}) {
  try {
    const res = await fetch(API + path, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    })
    const text = await res.text()
    let body = null
    try { body = text ? JSON.parse(text) : null } catch { body = text }
    return { status: res.status, ok: res.ok, body }
  } catch {
    return { status: 0, ok: false, body: null }
  }
}

const loadSession = () => { try { return JSON.parse(sessionStorage.getItem('session')) } catch { return null } }
const chipClass = (s) => (s >= 200 && s < 300 ? 'chip ok' : s === 0 || s >= 500 ? 'chip bad' : 'chip warn')

function Login({ onLogin }) {
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' })
  const [error, setError] = useState('')
  const submit = async (e) => {
    e.preventDefault()
    const r = await call('/api/auth/login', null, { method: 'POST', body: JSON.stringify(form) })
    if (r.ok) onLogin(r.body)
    else setError(r.status === 0 ? `Can't reach the gateway at ${API}.` : 'Wrong username or password.')
  }
  return (
    <div className="login-wrap">
      <section className="login-intro">
        <h1>API Gateway Console</h1>
        <p>One secured entry point for every backend service. Sign in to manage routes, change rate limits and test traffic.</p>
        <ul>
          <li>Routes you change go live in about 15 seconds, with no restart.</li>
          <li>Every request needs a valid login token.</li>
          <li>Each route has its own requests-per-minute limit.</li>
        </ul>
      </section>
      <form className="login-form" onSubmit={submit}>
        <h2>Sign in</h2>
        <label>Username<input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></label>
        <label>Password<input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
        {error && <p className="error">{error}</p>}
        <button className="primary">Sign in</button>
        <p className="hint">Demo admin account is filled in for you.</p>
      </form>
    </div>
  )
}

function Console({ session, onLogout }) {
  const token = session.token
  const isAdmin = session.role === 'ADMIN'
  const [routes, setRoutes] = useState([])
  const [form, setForm] = useState(blank)
  const [log, setLog] = useState([])
  const [counts, setCounts] = useState({ sent: 0, limited: 0 })
  const [busy, setBusy] = useState('')
  const [notice, setNotice] = useState(null)
  const [filter, setFilter] = useState('')

  const say = (kind, text) => { setNotice({ kind, text }); setTimeout(() => setNotice(null), 5000) }

  const load = async () => {
    const r = await call('/api/routes', token)
    if (r.status === 401) return onLogout()
    if (r.ok) setRoutes(r.body)
    else say('bad', `Can't reach the gateway at ${API}. Check that the containers are running.`)
  }
  useEffect(() => { load() }, [])

  const probe = async (r) => {
    const path = r.path.replace('/**', '')
    const t0 = performance.now()
    const res = await call(path, token)
    return { path, status: res.status, ms: Math.round(performance.now() - t0) }
  }
  const record = (route, text, status, ms, sent, limited) => {
    setLog((l) => [{ id: Math.random(), time: new Date().toLocaleTimeString(), route, text, status, ms }, ...l].slice(0, 12))
    setCounts((c) => ({ sent: c.sent + sent, limited: c.limited + limited }))
  }
  const sendOne = async (r) => {
    setBusy(r.routeId)
    const p = await probe(r)
    record(r.routeId, `GET ${p.path}`, p.status, p.ms, 1, p.status === 429 ? 1 : 0)
    setBusy('')
  }
  const burst = async (r) => {
    setBusy(r.routeId)
    const results = []
    for (let i = 0; i < 25; i++) results.push(await probe(r))
    const limited = results.filter((x) => x.status === 429).length
    const ok = results.filter((x) => x.status === 200).length
    const avg = Math.round(results.reduce((a, x) => a + x.ms, 0) / results.length)
    record(r.routeId, `25 quick requests: ${ok} passed, ${limited} blocked`, limited ? 429 : results[0].status, avg, 25, limited)
    setBusy('')
  }

  const saveLimit = async (r, value) => {
    const n = Math.max(1, parseInt(value, 10) || r.rateLimitPerMin)
    if (n === r.rateLimitPerMin) return
    const res = await call(`/api/routes/${r.id}`, token, { method: 'PUT', body: JSON.stringify({ ...r, rateLimitPerMin: n }) })
    if (res.ok) say('ok', `${r.routeId}: limit set to ${n} per minute. The gateway applies it within 15 seconds.`)
    else say('bad', 'Could not save the limit.')
    load()
  }
  const toggle = async (r) => {
    await call(`/api/routes/${r.id}`, token, { method: 'PUT', body: JSON.stringify({ ...r, enabled: !r.enabled }) })
    say('ok', `${r.routeId} ${r.enabled ? 'disabled' : 'enabled'}. The gateway applies it within 15 seconds.`)
    load()
  }
  const remove = async (r) => {
    if (!window.confirm(`Delete route ${r.routeId}?`)) return
    await call(`/api/routes/${r.id}`, token, { method: 'DELETE' })
    say('ok', `${r.routeId} deleted.`)
    load()
  }
  const add = async (e) => {
    e.preventDefault()
    const res = await call('/api/routes', token, { method: 'POST', body: JSON.stringify(form) })
    if (res.ok) { say('ok', `${form.routeId} added. It goes live within 15 seconds.`); setForm(blank); load() }
    else say('bad', res.status === 403 ? 'Only admins can add routes.' : 'Could not add the route. Route IDs must be unique.')
  }

  const shown = useMemo(
    () => routes.filter((r) => `${r.routeId} ${r.path} ${r.uri}`.toLowerCase().includes(filter.toLowerCase())),
    [routes, filter]
  )
  const enabled = routes.filter((r) => r.enabled).length

  return (
    <main>
      <header>
        <div>
          <h1>Gateway console</h1>
          <p className="sub">Gateway at {API}</p>
        </div>
        <div className="who">
          <span>{session.username}, {session.role.toLowerCase()}</span>
          <button className="ghost" onClick={onLogout}>Sign out</button>
        </div>
      </header>

      {notice && <div className={`notice ${notice.kind}`} role="status">{notice.text}</div>}

      <dl className="strip">
        <div><dt>Routes</dt><dd>{routes.length}</dd></div>
        <div><dt>Enabled</dt><dd>{enabled}</dd></div>
        <div><dt>Test requests sent</dt><dd>{counts.sent}</dd></div>
        <div><dt>Blocked by rate limit</dt><dd className={counts.limited ? 'warn-text' : ''}>{counts.limited}</dd></div>
      </dl>

      <div className="columns">
        <section className="panel">
          <div className="panel-head">
            <h2>Routes</h2>
            <input className="search" placeholder="Search routes" value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Search routes" />
          </div>
          <p className="hint">Changes reach the gateway within about 15 seconds. No restart needed.</p>
          <div className="scroll">
            <table>
              <thead><tr><th>Route</th><th>Path</th><th>Upstream</th><th>Limit per minute</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {shown.length === 0 && <tr><td colSpan="6" className="empty">No routes match. Add one below.</td></tr>}
                {shown.map((r) => (
                  <tr key={r.id}>
                    <td className="strong">{r.routeId}</td>
                    <td>{r.path}</td>
                    <td>{r.uri}</td>
                    <td>
                      {isAdmin
                        ? <input className="limit" type="number" min="1" key={`${r.id}-${r.rateLimitPerMin}`} defaultValue={r.rateLimitPerMin}
                            onBlur={(e) => saveLimit(r, e.target.value)} onKeyDown={(e) => e.key === 'Enter' && e.target.blur()} aria-label={`Limit for ${r.routeId}`} />
                        : r.rateLimitPerMin}
                    </td>
                    <td><span className={r.enabled ? 'pill on' : 'pill off'}>{r.enabled ? 'Enabled' : 'Disabled'}</span></td>
                    <td><div className="actions">
                      <button className="ghost" disabled={busy === r.routeId} onClick={() => sendOne(r)}>Send request</button>
                      <button className="ghost" disabled={busy === r.routeId} onClick={() => burst(r)}>Send 25 quickly</button>
                      {isAdmin && <button className="ghost" onClick={() => toggle(r)}>{r.enabled ? 'Disable' : 'Enable'}</button>}
                      {isAdmin && <button className="ghost danger" onClick={() => remove(r)}>Delete</button>}
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="panel">
          <h2>Request log</h2>
          <p className="hint">Results of the test requests you send from this page.</p>
          {log.length === 0 && <p className="empty">Nothing sent yet. Use Send request on a route.</p>}
          <ul className="log">
            {log.map((e) => (
              <li key={e.id}>
                <span className={chipClass(e.status)}>{e.status === 0 ? 'No reply' : e.status}</span>
                <div><strong>{e.route}</strong><span>{e.text}</span></div>
                <time>{e.time}<br />{e.ms} ms</time>
              </li>
            ))}
          </ul>
        </aside>
      </div>

      {isAdmin && (
        <form className="panel grid" onSubmit={add}>
          <h2>Add a route</h2>
          <label>Route ID<input required value={form.routeId} onChange={(e) => setForm({ ...form, routeId: e.target.value })} placeholder="shipping-service" /></label>
          <label>Path<input required value={form.path} onChange={(e) => setForm({ ...form, path: e.target.value })} placeholder="/shipping/**" /></label>
          <label>Upstream URL<input required value={form.uri} onChange={(e) => setForm({ ...form, uri: e.target.value })} placeholder="http://sample-backend:8082" /></label>
          <label>Requests per minute<input type="number" min="1" value={form.rateLimitPerMin} onChange={(e) => setForm({ ...form, rateLimitPerMin: +e.target.value })} /></label>
          <button className="primary">Add route</button>
        </form>
      )}

      <p className="hint foot">Live charts: Grafana at localhost:3000. Raw metrics: Prometheus at localhost:9090.</p>
    </main>
  )
}

export default function App() {
  const [session, setSession] = useState(loadSession)
  const login = (s) => { try { sessionStorage.setItem('session', JSON.stringify(s)) } catch { /* ignore */ } setSession(s) }
  const logout = () => { try { sessionStorage.removeItem('session') } catch { /* ignore */ } setSession(null) }
  return session ? <Console session={session} onLogout={logout} /> : <Login onLogin={login} />
}
