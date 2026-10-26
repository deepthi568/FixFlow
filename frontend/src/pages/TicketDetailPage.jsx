import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge.jsx';
import Toast from '../components/Toast.jsx';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import './TicketDetailPage.css';

const STATUSES = [
  'RECEIVED','DIAGNOSED','IN_REPAIR','QUALITY_CHECK',
  'READY','DELIVERED','ON_HOLD','CANCELLED',
];
const PROGRESSION = ['RECEIVED', 'DIAGNOSED', 'IN_REPAIR', 'QUALITY_CHECK', 'READY', 'DELIVERED'];

const PRIORITY_LABELS = { HIGH: '🔴 High', MEDIUM: '🟡 Medium', LOW: '🟢 Low' };

export default function TicketDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canEdit = hasRole('ADMIN', 'SERVICE_STAFF');
  const canAssign = hasRole('ADMIN', 'SERVICE_STAFF');
  const canDelete = hasRole('ADMIN');
  const canUseParts = hasRole('ADMIN', 'SERVICE_STAFF', 'TECHNICIAN');
  const quickStatuses = hasRole('TECHNICIAN')
    ? ['DIAGNOSED', 'IN_REPAIR', 'QUALITY_CHECK', 'ON_HOLD', 'READY']
    : STATUSES;

  const [ticket,      setTicket]     = useState(null);
  const [technicians, setTechs]      = useState([]);
  const [editing,     setEditing]    = useState(false);
  const [form,        setForm]       = useState({});
  const [saving,      setSaving]     = useState(false);
  const [loading,     setLoading]    = useState(true);
  const [error,       setError]      = useState('');
  const [toast,       setToast]      = useState({ msg: '', type: 'success' });
  const [deleting,    setDeleting]   = useState(false);
  const [parts,       setParts]      = useState([]);
  const [partUsage,   setPartUsage]  = useState([]);
  const [partForm,    setPartForm]   = useState({ itemId: '', quantity: 1, note: '' });

  const showToast = useCallback((msg, type = 'success') => setToast({ msg, type }), []);

  const loadTicket = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get(`/api/tickets/${id}`);
      setTicket(data);
      setForm(ticketToForm(data));
    } catch (err) {
      setError(err.response?.data?.message || 'Ticket not found.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadTicket(); }, [loadTicket]);

  const loadParts = useCallback(async () => {
    const [{ data: available }, { data: used }] = await Promise.all([
      api.get('/api/inventory'),
      api.get(`/api/inventory/tickets/${id}/movements`),
    ]);
    setParts(available);
    setPartUsage(used);
    setPartForm((prev) => ({ ...prev, itemId: prev.itemId || String(available[0]?.id || '') }));
  }, [id]);

  useEffect(() => { loadParts().catch(() => {}); }, [loadParts]);

  async function recordPartUsage(e) {
    e.preventDefault();
    try {
      await api.post(`/api/tickets/${id}/parts`, {
        itemId: Number(partForm.itemId), quantity: Number(partForm.quantity), note: partForm.note,
      });
      showToast('Part usage recorded and stock updated.');
      setPartForm((prev) => ({ ...prev, note: '' }));
      await loadParts();
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not record part usage.', 'error');
    }
  }

  useEffect(() => {
    if (!canAssign) return;
    api.get('/api/technicians').then(r => setTechs(r.data)).catch(() => setTechs([]));
  }, [canAssign]);

  function ticketToForm(t) {
    return {
      customerName:        t.customerName        ?? '',
      customerPhone:       t.customerPhone        ?? '',
      customerEmail:       t.customerEmail        ?? '',
      deviceName:          t.deviceName           ?? '',
      problemDescription:  t.problemDescription   ?? '',
      status:              t.status               ?? 'RECEIVED',
      priority:            t.priority             ?? 'MEDIUM',
      targetCompletionDate:t.targetCompletionDate ?? '',
      estimatedCost:       t.estimatedCost != null ? String(t.estimatedCost) : '',
      technicianId:        t.technician?.id       ?? '',
    };
  }

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
  }

  function cancelEdit() {
    setForm(ticketToForm(ticket));
    setEditing(false);
    setError('');
  }

  // ── Validation ────────────────────────────────────────────────────────
  function validate() {
    if (!form.customerName.trim())       return 'Customer name is required.';
    if (!form.customerPhone.trim())      return 'Customer phone is required.';
    if (!form.deviceName.trim())         return 'Device name is required.';
    if (!form.problemDescription.trim()) return 'Problem description is required.';
    if (form.estimatedCost !== '' && Number(form.estimatedCost) < 0)
      return 'Estimated cost must not be negative.';
    return null;
  }

  // ── Save full edit ────────────────────────────────────────────────────
  async function handleSave(e) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }

    setSaving(true);
    setError('');
    try {
      const payload = {
        customerName:        form.customerName.trim(),
        customerPhone:       form.customerPhone.trim(),
        customerEmail:       form.customerEmail.trim() || null,
        deviceName:          form.deviceName.trim(),
        problemDescription:  form.problemDescription.trim(),
        status:              form.status,
        priority:            form.priority,
        targetCompletionDate:form.targetCompletionDate || null,
        estimatedCost:       form.estimatedCost !== '' ? Number(form.estimatedCost) : null,
      };

      const { data: updated } = await api.put(`/api/tickets/${id}`, payload);

      // Update technician separately
      const newTechId = form.technicianId || null;
      const oldTechId = ticket.technician?.id ?? null;
      if (String(newTechId) !== String(oldTechId)) {
        const params = newTechId ? { technicianId: newTechId } : {};
        await api.patch(`/api/tickets/${id}/technician`, null, { params });
      }

      setTicket(updated);
      setForm(ticketToForm(updated));
      setEditing(false);
      showToast('Ticket updated successfully.');
      // Reload to get technician relation
      loadTicket();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save changes. Please try again.');
    } finally {
      setSaving(false);
    }
  }

  // ── Quick status change ───────────────────────────────────────────────
  async function handleQuickStatus(status) {
    try {
      const { data } = await api.patch(`/api/tickets/${id}/status`, null, { params: { status } });
      setTicket(data);
      setForm(prev => ({ ...prev, status: data.status }));
      showToast(`Status updated to ${status.replace(/_/g, ' ')}.`);
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not update status.', 'error');
    }
  }

  // ── Delete ────────────────────────────────────────────────────────────
  async function handleDelete() {
    if (!window.confirm(`Archive ticket #${id}? It will be hidden but kept in the database.`)) return;
    setDeleting(true);
    try {
      await api.delete(`/api/tickets/${id}`);
      showToast(`Ticket #${id} archived.`);
      setTimeout(() => navigate('/tickets'), 1200);
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not archive ticket.', 'error');
      setDeleting(false);
    }
  }

  // ── Render ────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="page">
        <div className="skeleton-rows" style={{ maxWidth: 700 }}>
          {[1,2,3,4,5].map(i => <div key={i} className="skeleton-row" />)}
        </div>
      </div>
    );
  }

  if (error && !ticket) {
    return (
      <div className="page">
        <div className="error-state">
          <div className="error-icon">⚠</div>
          <h2>Ticket not found</h2>
          <p>{error}</p>
          <Link to="/tickets" className="btn-primary" style={{ display: 'inline-block', marginTop: 16 }}>
            ← Back to Tickets
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      {/* ── Breadcrumb + Actions ─────────────────────────────── */}
      <header className="topbar">
        <div>
          <nav className="breadcrumb">
            <Link to="/tickets">Tickets</Link>
            <span> / </span>
            <span>#{ticket.id}</span>
          </nav>
          <h1>Ticket #{ticket.id}</h1>
        </div>
        <div className="topbar-right">
          {!editing && (
            <>
              {canEdit && (
                <button
                  className="btn-outline"
                  onClick={() => { setEditing(true); setError(''); }}
                >
                  ✎ Edit
                </button>
              )}
              {canDelete && (
                <button
                  className="btn-danger"
                  onClick={handleDelete}
                  disabled={deleting}
                >
                  {deleting ? 'Archiving…' : '🗑 Archive'}
                </button>
              )}
            </>
          )}
        </div>
      </header>

      {error && editing && (
        <div className="form-error" role="alert">{error}</div>
      )}

      <section className="panel ticket-progress" aria-label="Ticket status progression">
        <div className="panel-heading"><div><p className="eyebrow">WORKFLOW</p><h2>Repair progression</h2></div><StatusBadge status={ticket.status} /></div>
        {['ON_HOLD', 'CANCELLED'].includes(ticket.status) && <p className="progress-note">This ticket is currently {ticket.status === 'ON_HOLD' ? 'on hold' : 'cancelled'}.</p>}
        <ol className="progress-steps">
          {PROGRESSION.map((status, index) => {
            const currentIndex = PROGRESSION.indexOf(ticket.status);
            const state = currentIndex === index ? 'current' : currentIndex > index ? 'complete' : 'upcoming';
            return <li key={status} className={`progress-step progress-${state}`} aria-current={state === 'current' ? 'step' : undefined}>
              <span className="progress-marker">{state === 'complete' ? '✓' : index + 1}</span>
              <span>{status.replace(/_/g, ' ')}</span>
            </li>;
          })}
        </ol>
      </section>

      <div className="detail-grid">
        {/* ── Left: main form ─────────────────────────────────── */}
        <form className="panel detail-form" onSubmit={handleSave} noValidate>
          <div className="panel-heading">
            <div>
              <p className="eyebrow">TICKET DETAILS</p>
              <h2>{editing ? 'Edit Ticket' : 'Ticket Information'}</h2>
            </div>
            {editing && (
              <div className="topbar-right">
                <button type="button" className="btn-outline" onClick={cancelEdit} disabled={saving}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Saving…' : '✓ Save Changes'}
                </button>
              </div>
            )}
          </div>

          {/* Customer */}
          <section className="form-section">
            <h3 className="section-title">Customer</h3>
            <div className="form-row">
              <Field label="Name" req editing={editing}>
                {editing
                  ? <input name="customerName" value={form.customerName} onChange={handleChange} required />
                  : <span>{ticket.customerName}</span>}
              </Field>
              <Field label="Phone" req editing={editing}>
                {editing
                  ? <input name="customerPhone" type="tel" value={form.customerPhone} onChange={handleChange} required />
                  : <span>{ticket.customerPhone || '—'}</span>}
              </Field>
            </div>
            <Field label="Email" editing={editing}>
              {editing
                ? <input name="customerEmail" type="email" value={form.customerEmail} onChange={handleChange} />
                : <span>{ticket.customerEmail || <em className="subtext">—</em>}</span>}
            </Field>
          </section>

          {/* Device */}
          <section className="form-section">
            <h3 className="section-title">Device</h3>
            <Field label="Device / Asset" req editing={editing}>
              {editing
                ? <input name="deviceName" value={form.deviceName} onChange={handleChange} required />
                : <span>{ticket.deviceName}</span>}
            </Field>
            <Field label="Problem Description" req editing={editing}>
              {editing
                ? <textarea name="problemDescription" value={form.problemDescription}
                    onChange={handleChange} rows={4} required />
                : <p className="detail-text">{ticket.problemDescription}</p>}
            </Field>
          </section>

          {/* Workflow */}
          <section className="form-section">
            <h3 className="section-title">Workflow</h3>
            <div className="form-row">
              <Field label="Status" req editing={editing}>
                {editing
                  ? <select name="status" value={form.status} onChange={handleChange}>
                      {STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                    </select>
                  : <StatusBadge status={ticket.status} />}
              </Field>
              <Field label="Priority" req editing={editing}>
                {editing
                  ? <select name="priority" value={form.priority} onChange={handleChange}>
                      <option value="HIGH">🔴 High</option>
                      <option value="MEDIUM">🟡 Medium</option>
                      <option value="LOW">🟢 Low</option>
                    </select>
                  : <span className={`priority-badge priority-${ticket.priority?.toLowerCase()}`}>
                      {PRIORITY_LABELS[ticket.priority] ?? ticket.priority}
                    </span>}
              </Field>
            </div>
            <div className="form-row">
              <Field label="Assigned Technician" editing={editing}>
                {editing
                  ? <select name="technicianId" value={form.technicianId} onChange={handleChange}>
                      <option value="">— Unassigned —</option>
                      {technicians.map(t => (
                        <option key={t.id} value={t.id}>{t.name}{t.specialty ? ` · ${t.specialty}` : ''}</option>
                      ))}
                    </select>
                  : <span>{ticket.technician?.name ?? <em className="subtext">Unassigned</em>}</span>}
              </Field>
              <Field label="Target Completion" editing={editing}>
                {editing
                  ? <input name="targetCompletionDate" type="date" value={form.targetCompletionDate}
                      onChange={handleChange} min={new Date().toISOString().split('T')[0]} />
                  : <span>
                      {ticket.targetCompletionDate
                        ? new Date(ticket.targetCompletionDate + 'T00:00:00').toLocaleDateString()
                        : <em className="subtext">Not set</em>}
                    </span>}
              </Field>
            </div>
            <Field label="Estimated Cost (₹)" editing={editing}>
              {editing
                ? <input name="estimatedCost" type="number" min="0" step="0.01"
                    value={form.estimatedCost} onChange={handleChange} placeholder="0.00" />
                : <span>
                    {ticket.estimatedCost != null
                      ? `₹${Number(ticket.estimatedCost).toFixed(2)}`
                      : <em className="subtext">Not set</em>}
                  </span>}
            </Field>
          </section>
        </form>

        {/* ── Right: meta + quick status ──────────────────────── */}
        <aside>
          {/* Meta card */}
          <div className="panel meta-card">
            <p className="eyebrow">METADATA</p>
            <dl className="meta-list">
              <dt>Ticket ID</dt>
              <dd>#{ticket.id}</dd>
              <dt>Created</dt>
              <dd>{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : '—'}</dd>
              <dt>Last Updated</dt>
              <dd>{ticket.updatedAt ? new Date(ticket.updatedAt).toLocaleString() : '—'}</dd>
            </dl>
          </div>

          {canUseParts && (
            <div className="panel status-card">
              <p className="eyebrow">SPARE PARTS</p>
              <form onSubmit={recordPartUsage}>
                <div className="form-group">
                  <label htmlFor="ticket-part">Part</label>
                  <select id="ticket-part" required value={partForm.itemId} onChange={(e) => setPartForm({ ...partForm, itemId: e.target.value })}>
                    {parts.map((part) => <option key={part.id} value={part.id}>{part.name} · {part.quantity} in stock</option>)}
                  </select>
                </div>
                <div className="form-group"><label htmlFor="ticket-part-quantity">Quantity</label><input id="ticket-part-quantity" type="number" min="1" required value={partForm.quantity} onChange={(e) => setPartForm({ ...partForm, quantity: e.target.value })} /></div>
                <div className="form-group"><label htmlFor="ticket-part-note">Note</label><input id="ticket-part-note" value={partForm.note} onChange={(e) => setPartForm({ ...partForm, note: e.target.value })} placeholder="Optional" /></div>
                <button className="btn-primary" type="submit" disabled={parts.length === 0}>Record usage</button>
              </form>
              <div style={{ marginTop: 18 }}>
                <strong>Parts used</strong>
                {partUsage.filter((entry) => entry.type === 'CONSUMPTION').length === 0
                  ? <p className="subtext">No parts recorded for this ticket.</p>
                  : partUsage.filter((entry) => entry.type === 'CONSUMPTION').map((entry) => (
                    <p key={entry.id} className="subtext">{entry.itemName}: {Math.abs(entry.quantityChange)} · {entry.performedBy} · {new Date(entry.createdAt).toLocaleDateString()}</p>
                  ))}
              </div>
            </div>
          )}

          {/* Quick status update */}
          {!editing && (
            <div className="panel status-card">
              <p className="eyebrow">QUICK STATUS UPDATE</p>
              <p className="status-hint">Current: <StatusBadge status={ticket.status} /></p>
              <div className="status-grid">
                {quickStatuses.filter(s => s !== ticket.status).map(s => (
                  <button
                    key={s}
                    className="status-btn"
                    onClick={() => handleQuickStatus(s)}
                  >
                    {s.replace(/_/g, ' ')}
                  </button>
                ))}
              </div>
            </div>
          )}
        </aside>
      </div>

      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}

/** Reusable field wrapper for view/edit mode. */
function Field({ label, req, editing, children }) {
  return (
    <div className={`form-group${editing ? ' editing' : ''}`}>
      <label>
        {label}
        {req && editing && <span className="req"> *</span>}
      </label>
      {children}
    </div>
  );
}
