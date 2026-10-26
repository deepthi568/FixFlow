import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client.js';
import Toast from '../components/Toast.jsx';
import './TicketsPage.css';

const EMPTY = {
  username: '',
  password: '',
  fullName: '',
  role: 'SERVICE_STAFF',
  technicianId: '',
};

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [toast, setToast] = useState({ msg: '', type: 'success' });
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    const [userRes, techRes] = await Promise.all([
      api.get('/api/users'),
      api.get('/api/technicians'),
    ]);
    setUsers(userRes.data);
    setTechnicians(techRes.data);
  }, []);

  useEffect(() => {
    load().catch(() => setToast({ msg: 'Could not load users.', type: 'error' }));
  }, [load]);

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    try {
      const payload = {
        username: form.username.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        role: form.role,
        technicianId: form.role === 'TECHNICIAN' && form.technicianId ? Number(form.technicianId) : null,
      };
      await api.post('/api/users', payload);
      setForm(EMPTY);
      setToast({ msg: 'User created.', type: 'success' });
      await load();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create user.');
    }
  }

  async function toggleEnabled(user) {
    try {
      await api.put(`/api/users/${user.id}`, { enabled: !user.enabled });
      await load();
    } catch (err) {
      setToast({ msg: err.response?.data?.message || 'Could not update user.', type: 'error' });
    }
  }

  return (
    <div className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">ADMINISTRATION</p>
          <h1>Users</h1>
        </div>
      </header>

      <div className="panel" style={{ marginBottom: 22 }}>
        <h2 style={{ fontSize: 16, marginBottom: 16 }}>Create account</h2>
        {error && <div className="form-error" role="alert">{error}</div>}
        <form onSubmit={handleCreate} className="filters-bar" style={{ alignItems: 'end' }}>
          <input className="search-input" required minLength={3} placeholder="Username" value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })} />
          <input className="search-input" required minLength={8} type="password" placeholder="Password (min 8)" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <input className="search-input" placeholder="Full name" value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          <select className="inline-select" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="ADMIN">ADMIN</option>
            <option value="SERVICE_STAFF">SERVICE_STAFF</option>
            <option value="TECHNICIAN">TECHNICIAN</option>
          </select>
          {form.role === 'TECHNICIAN' && (
            <select required value={form.technicianId}
              onChange={(e) => setForm({ ...form, technicianId: e.target.value })}>
              <option value="">Link technician…</option>
              {technicians.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          )}
          <button className="btn-primary" type="submit">Create</button>
        </form>
      </div>

      <div className="panel tickets-panel">
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Name</th>
                <th>Role</th>
                <th>Technician</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.username}</strong></td>
                  <td>{u.fullName || '—'}</td>
                  <td>{u.role}</td>
                  <td>{u.technicianId ?? '—'}</td>
                  <td>{u.enabled ? 'Active' : 'Disabled'}</td>
                  <td>
                    <button className="btn-outline" onClick={() => toggleEnabled(u)}>
                      {u.enabled ? 'Disable' : 'Enable'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}
