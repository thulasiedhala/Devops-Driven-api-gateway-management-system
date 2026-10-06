import { useEffect, useState } from 'react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const empty = { routeId: '', path: '', uri: '', stripPrefix: 0, rateLimitPerMin: 100, enabled: true }

async function call(path, token, options = {}) {
  const res = await fetch(API + path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  })
  const text = await res.text()
  return { status: res.status, ok: res.ok, body: text ? JSON.parse(text) : null }
}

function Login({ onLogin }) {
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' })
  const [error, setError] = useState('')
  const submit = async (e) => {
    e.preventDefault()
    const r = await call('/api/auth/login', null, { method: 'POST', body: JSON.stringify(form) })
    r.ok ? onLogin(r.body) : setError('Wrong username or password.')
  }
  return (
    <form className="panel login" onSubmit={submit}>
      <h1>Gateway Console</h1>
      <label>Username<input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></label>
      <label>Password<input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
      {error && <p className="error">{error}</p>}
      <button>Sign in</button>
    </form>
  )
}

export default function App() {
  const [session, setSession] = useState(null)
  const [routes, setRoutes] = useState([])
  const [form, setForm] = useState(empty)
  const [probe, setProbe] = useState('')
  const token = session?.token
  const isAdmin = session?.role === 'ADMIN'

  const load = async () => {
    const r = await call('/api/routes', token)
    if (r.ok) setRoutes(r.body)
  }
  useEffect(() => { if (token) load() }, [token])

  if (!session) return <Login onLogin={setSession} />

  const add = async (e) => {
    e.preventDefault()
    const r = await call('/api/routes', token, { method: 'POST', body: JSON.stringify(form) })
    if (r.ok) { setForm(empty); load() }
  }
  const toggle = async (r) => {
    await call(`/api/routes/${r.id}`, token, { method: 'PUT', body: JSON.stringify({ ...r, enabled: !r.enabled }) })
    load()
  }
  const remove = async (r) => { await call(`/api/routes/${r.id}`, token, { method: 'DELETE' }); load() }
  const test = async (r) => {
    const base = r.path.replace('/**', '')
    const res = await call(base, token)
    setProbe(`GET ${base} → ${res.status}${res.status === 429 ? ' (rate limit hit)' : ''}`)
  }

  return (
    <main>
      <header>
        <h1>Gateway Console</h1>
        <span>{session.username} · {session.role} <button className="ghost" onClick={() => setSession(null)}>Sign out</button></span>
      </header>

      <section className="panel">
        <h2>Routes</h2>
        <p className="hint">Changes reach the gateway within about 15 seconds. No restart needed.</p>
        <table>
          <thead><tr><th>ID</th><th>Path</th><th>Upstream</th><th>Limit / min</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {routes.length === 0 && <tr><td colSpan="6">No routes yet. Add one below.</td></tr>}
            {routes.map((r) => (
              <tr key={r.id}>
                <td>{r.routeId}</td><td>{r.path}</td><td>{r.uri}</td><td>{r.rateLimitPerMin}</td>
                <td className={r.enabled ? 'on' : 'off'}>{r.enabled ? 'Enabled' : 'Disabled'}</td>
                <td className="actions">
                  <button className="ghost" onClick={() => test(r)}>Send test request</button>
                  {isAdmin && <button className="ghost" onClick={() => toggle(r)}>{r.enabled ? 'Disable' : 'Enable'}</button>}
                  {isAdmin && <button className="danger" onClick={() => remove(r)}>Delete</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {probe && <p className="probe">{probe}</p>}
      </section>

      {isAdmin && (
        <form className="panel grid" onSubmit={add}>
          <h2>Add route</h2>
          <label>Route ID<input required value={form.routeId} onChange={(e) => setForm({ ...form, routeId: e.target.value })} placeholder="order-service" /></label>
          <label>Path<input required value={form.path} onChange={(e) => setForm({ ...form, path: e.target.value })} placeholder="/orders/**" /></label>
          <label>Upstream URL<input required value={form.uri} onChange={(e) => setForm({ ...form, uri: e.target.value })} placeholder="http://sample-backend:8082" /></label>
          <label>Requests per minute<input type="number" min="1" value={form.rateLimitPerMin} onChange={(e) => setForm({ ...form, rateLimitPerMin: +e.target.value })} /></label>
          <button>Add route</button>
        </form>
      )}

      <p className="hint">Live traffic and latency: Grafana at localhost:3000. Raw metrics: Prometheus at localhost:9090.</p>
    </main>
  )
}
