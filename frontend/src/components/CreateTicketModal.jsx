import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import './CreateTicketModal.css';

const EMPTY_FORM = {
  customerName:        '',
  customerPhone:       '',
  customerEmail:       '',
  deviceName:          '',
  problemDescription:  '',
  estimatedCost:       '',
  priority:            'MEDIUM',
  targetCompletionDate:'',
  technicianId:        '',
};

/**
 * Modal for creating a new repair ticket.
 * Props:
 *   open       – boolean
 *   onClose    – () => void
 *   onSuccess  – (message: string) => void   called after successful creation
 */
export default function CreateTicketModal({ open, onClose, onSuccess }) {
  const [form, setForm]         = useState(EMPTY_FORM);
  const [technicians, setTechs] = useState([]);
  const [saving, setSaving]     = useState(false);
  const [error, setError]       = useState('');

  /* Load technician list when modal opens */
  useEffect(() => {
    if (!open) return;
    api.get('/api/technicians')
      .then(r => setTechs(r.data))
      .catch(() => setTechs([]));
  }, [open]);

  /* Keyboard: Escape closes */
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (e.key === 'Escape') handleClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!open) return null;

  function handleClose() {
    setForm(EMPTY_FORM);
    setError('');
    onClose();
  }

  function handleChange(e) {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError('');

    // Frontend validation
    if (!form.customerName.trim())       { setError('Customer name is required.');        setSaving(false); return; }
    if (!form.customerPhone.trim())      { setError('Customer phone is required.');       setSaving(false); return; }
    if (!form.deviceName.trim())         { setError('Device / asset name is required.');  setSaving(false); return; }
    if (!form.problemDescription.trim()) { setError('Problem description is required.');  setSaving(false); return; }
    if (form.estimatedCost !== '' && Number(form.estimatedCost) < 0) {
      setError('Estimated cost must not be negative.'); setSaving(false); return;
    }

    const payload = {
      customerName:        form.customerName.trim(),
      customerPhone:       form.customerPhone.trim(),
      customerEmail:       form.customerEmail.trim() || null,
      deviceName:          form.deviceName.trim(),
      problemDescription:  form.problemDescription.trim(),
      estimatedCost:       form.estimatedCost !== '' ? Number(form.estimatedCost) : null,
      priority:            form.priority || 'MEDIUM',
      targetCompletionDate:form.targetCompletionDate || null,
    };

    try {
      const { data: created } = await api.post('/api/tickets', payload);

      if (form.technicianId) {
        await api.patch(
          `/api/tickets/${created.id}/technician`,
          null,
          { params: { technicianId: form.technicianId } }
        );
      }

      onSuccess('Repair ticket created successfully.');
      handleClose();
    } catch (err) {
      const msg = err.response?.data?.message
        || 'Could not create the ticket. Check all required fields.';
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <dialog
      className="modal-overlay"
      open
      aria-modal="true"
      onClick={handleClose}
      onKeyDown={(event) => { if (event.key === 'Escape') handleClose() }}
    >
      <section
        className="modal-card"
        onClick={e => e.stopPropagation()}
        aria-labelledby="modal-title"
      >
        <div className="modal-header">
          <div>
            <p className="eyebrow">NEW REQUEST</p>
            <h2 id="modal-title">Create Repair Ticket</h2>
          </div>
          <button className="modal-close" onClick={handleClose} aria-label="Close modal">×</button>
        </div>

        {error && <div className="modal-error" role="alert">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          {/* Row 1: Customer name + Phone */}
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="mc-customerName">Customer Name <span className="req">*</span></label>
              <input
                id="mc-customerName"
                name="customerName"
                value={form.customerName}
                onChange={handleChange}
                placeholder="Full name"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="mc-customerPhone">Phone <span className="req">*</span></label>
              <input
                id="mc-customerPhone"
                name="customerPhone"
                type="tel"
                value={form.customerPhone}
                onChange={handleChange}
                placeholder="+91 99999 00000"
                required
              />
            </div>
          </div>

          {/* Row 2: Email + Device */}
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="mc-customerEmail">Email <span className="optional">(optional)</span></label>
              <input
                id="mc-customerEmail"
                name="customerEmail"
                type="email"
                value={form.customerEmail}
                onChange={handleChange}
                placeholder="customer@example.com"
              />
            </div>
            <div className="form-group">
              <label htmlFor="mc-deviceName">Device / Asset <span className="req">*</span></label>
              <input
                id="mc-deviceName"
                name="deviceName"
                value={form.deviceName}
                onChange={handleChange}
                placeholder="e.g. Dell Inspiron 15"
                required
              />
            </div>
          </div>

          {/* Problem description */}
          <div className="form-group">
            <label htmlFor="mc-problemDescription">Reported Problem <span className="req">*</span></label>
            <textarea
              id="mc-problemDescription"
              name="problemDescription"
              value={form.problemDescription}
              onChange={handleChange}
              placeholder="Describe the fault in detail..."
              rows={3}
              required
            />
          </div>

          {/* Row 3: Priority + Target completion date */}
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="mc-priority">Priority <span className="req">*</span></label>
              <select
                id="mc-priority"
                name="priority"
                value={form.priority}
                onChange={handleChange}
              >
                <option value="HIGH">🔴 High</option>
                <option value="MEDIUM">🟡 Medium</option>
                <option value="LOW">🟢 Low</option>
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="mc-targetDate">Target Completion <span className="optional">(optional)</span></label>
              <input
                id="mc-targetDate"
                name="targetCompletionDate"
                type="date"
                value={form.targetCompletionDate}
                onChange={handleChange}
                min={new Date().toISOString().split('T')[0]}
              />
            </div>
          </div>

          {/* Row 4: Cost + Technician */}
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="mc-estimatedCost">Estimated Cost ₹ <span className="optional">(optional)</span></label>
              <input
                id="mc-estimatedCost"
                name="estimatedCost"
                type="number"
                min="0"
                step="0.01"
                value={form.estimatedCost}
                onChange={handleChange}
                placeholder="0.00"
              />
            </div>
            <div className="form-group">
              <label htmlFor="mc-technician">Assign Technician <span className="optional">(optional)</span></label>
              <select
                id="mc-technician"
                name="technicianId"
                value={form.technicianId}
                onChange={handleChange}
              >
                <option value="">— Unassigned —</option>
                {technicians.map(t => (
                  <option key={t.id} value={t.id}>{t.name}{t.specialty ? ` · ${t.specialty}` : ''}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={handleClose}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Creating…' : '＋ Create Ticket'}
            </button>
          </div>
        </form>
      </section>
    </dialog>
  );
}
