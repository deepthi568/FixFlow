import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client.js';
import Toast from '../components/Toast.jsx';
import './TicketsPage.css';

const EMPTY = { name: '', email: '', phone: '', specialty: '', skills: '', availability: 'AVAILABLE' };

export default function TechniciansPage() {
  const [technicians, setTechnicians] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);
  const [toast, setToast] = useState({ msg: '', type: 'success' });

  const load = useCallback(async () => {
    const { data } = await api.get('/api/technicians/all');
    setTechnicians(data);
  }, []);

  useEffect(() => { load().catch(() => setToast({ msg: 'Could not load technicians.', type: 'error' })); }, [load]);

  function edit(technician) {
    setEditingId(technician.id);
    setForm({ name: technician.name || '', email: technician.email || '', phone: technician.phone || '', specialty: technician.specialty || '', skills: technician.skills || '', availability: technician.availability || 'AVAILABLE' });
  }

  function cancelEdit() { setEditingId(null); setForm(EMPTY); }

  async function save(e) {
    e.preventDefault();
    try {
      if (editingId) await api.put(`/api/technicians/${editingId}`, form);
      else await api.post('/api/technicians', form);
      setToast({ msg: editingId ? 'Technician profile updated.' : 'Technician added.', type: 'success' });
      cancelEdit();
      await load();
    } catch (err) {
      setToast({ msg: err.response?.data?.message || 'Could not save technician.', type: 'error' });
    }
  }

  async function deactivate(technician) {
    if (!window.confirm(`Deactivate ${technician.name}? Existing tickets stay assigned.`)) return;
    try {
      await api.delete(`/api/technicians/${technician.id}`);
      setToast({ msg: 'Technician deactivated.', type: 'success' });
      await load();
    } catch (err) {
      setToast({ msg: err.response?.data?.message || 'Could not deactivate technician.', type: 'error' });
    }
  }

  return (
    <div className="page">
      <header className="topbar"><div><p className="eyebrow">TEAM</p><h1>Technicians</h1></div></header>
      <form className="panel" onSubmit={save} style={{ padding: 22, marginBottom: 22 }}>
        <h2 style={{ marginTop: 0 }}>{editingId ? 'Edit technician' : 'Add technician'}</h2>
        <div className="form-row">
          <div className="form-group"><label htmlFor="technician-name">Name</label><input id="technician-name" required maxLength="128" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
          <div className="form-group"><label htmlFor="technician-email">Email</label><input id="technician-email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
          <div className="form-group"><label htmlFor="technician-phone">Phone</label><input id="technician-phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
          <div className="form-group"><label htmlFor="technician-specialty">Specialty</label><input id="technician-specialty" value={form.specialty} onChange={(e) => setForm({ ...form, specialty: e.target.value })} placeholder="e.g. Mobile devices" /></div>
        </div>
        <div className="form-row">
          <div className="form-group"><label htmlFor="technician-skills">Skills</label><textarea id="technician-skills" rows="2" maxLength="1000" value={form.skills} onChange={(e) => setForm({ ...form, skills: e.target.value })} placeholder="Board repair, diagnostics, soldering" /></div>
          <div className="form-group"><label htmlFor="technician-availability">Availability</label><select id="technician-availability" value={form.availability} onChange={(e) => setForm({ ...form, availability: e.target.value })}><option value="AVAILABLE">Available</option><option value="BUSY">Busy</option><option value="UNAVAILABLE">Unavailable</option></select></div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
          {editingId && <button className="btn-outline" type="button" onClick={cancelEdit}>Cancel</button>}
          <button className="btn-primary" type="submit">{editingId ? 'Save changes' : 'Add technician'}</button>
        </div>
      </form>

      <div className="panel tickets-panel"><div className="table-wrapper"><table>
        <thead><tr><th>Technician</th><th>Contact</th><th>Skills</th><th>Availability</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>{technicians.map((technician) => <tr key={technician.id}>
          <td><strong>{technician.name}</strong><div className="subtext">{technician.specialty || 'General repair'}</div></td>
          <td>{technician.email || '—'}<div className="subtext">{technician.phone || ''}</div></td>
          <td>{technician.skills || '—'}</td>
          <td>{(technician.availability || 'AVAILABLE').replace('_', ' ')}</td>
          <td>{technician.active ? 'Active' : 'Inactive'}</td>
          <td><div style={{ display: 'flex', gap: 8 }}><button className="btn-outline" type="button" onClick={() => edit(technician)}>Edit</button>{technician.active && <button className="btn-delete" type="button" onClick={() => deactivate(technician)}>Deactivate</button>}</div></td>
        </tr>)}
        {technicians.length === 0 && <tr><td colSpan="6" className="subtext">No technicians yet.</td></tr>}</tbody>
      </table></div></div>
      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}
