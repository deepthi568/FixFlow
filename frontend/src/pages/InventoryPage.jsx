import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client.js';
import Toast from '../components/Toast.jsx';
import './TicketsPage.css';

const EMPTY = { name: '', sku: '', description: '', quantity: 0, reorderThreshold: 0, unit: 'pcs' };

export default function InventoryPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [stockChanges, setStockChanges] = useState({});
  const [history, setHistory] = useState({});
  const [toast, setToast] = useState({ msg: '', type: 'success' });

  const load = useCallback(async () => {
    const { data } = await api.get('/api/inventory');
    setItems(data);
  }, []);

  useEffect(() => { load().catch(() => setToast({ msg: 'Could not load inventory.', type: 'error' })); }, [load]);

  async function handleCreate(e) {
    e.preventDefault();
    try {
      await api.post('/api/inventory', { ...form, quantity: Number(form.quantity), reorderThreshold: Number(form.reorderThreshold) });
      setForm(EMPTY);
      setToast({ msg: 'Spare part added.', type: 'success' });
      await load();
    } catch (err) {
      setToast({ msg: err.response?.data?.message || 'Could not save part.', type: 'error' });
    }
  }

  async function adjustStock(item) {
    const change = Number(stockChanges[item.id]);
    if (!Number.isInteger(change) || change === 0) {
      setToast({ msg: 'Enter a non-zero whole number to add or remove stock.', type: 'error' });
      return;
    }
    try {
      await api.post(`/api/inventory/${item.id}/stock-adjustments`, { quantityChange: change, note: 'Manual stock adjustment' });
      setStockChanges((prev) => ({ ...prev, [item.id]: '' }));
      setToast({ msg: 'Stock updated and recorded.', type: 'success' });
      await load();
      if (history[item.id]) await loadHistory(item.id);
    } catch (err) {
      setToast({ msg: err.response?.data?.message || 'Could not update stock.', type: 'error' });
    }
  }

  async function loadHistory(id) {
    try {
      const { data } = await api.get(`/api/inventory/${id}/movements`);
      setHistory((prev) => ({ ...prev, [id]: data }));
    } catch {
      setToast({ msg: 'Could not load stock history.', type: 'error' });
    }
  }

  async function toggleHistory(id) {
    if (history[id]) setHistory((prev) => ({ ...prev, [id]: null }));
    else await loadHistory(id);
  }

  return (
    <div className="page">
      <header className="topbar">
        <div><p className="eyebrow">PARTS &amp; SUPPLIES</p><h1>Inventory</h1></div>
      </header>

      {items.some((item) => item.quantity <= item.reorderThreshold) && (
        <div className="form-error" role="status" style={{ marginBottom: 18 }}>
          Low stock: {items.filter((item) => item.quantity <= item.reorderThreshold).map((item) => item.name).join(', ')}
        </div>
      )}

      <form className="panel filters-bar" onSubmit={handleCreate} style={{ marginBottom: 22, alignItems: 'end' }}>
        <input required maxLength="128" placeholder="Part name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input maxLength="64" placeholder="SKU" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} />
        <input maxLength="500" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <input aria-label="Initial quantity" type="number" min="0" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
        <input aria-label="Reorder threshold" title="Reorder threshold" type="number" min="0" value={form.reorderThreshold} onChange={(e) => setForm({ ...form, reorderThreshold: e.target.value })} />
        <button className="btn-primary" type="submit">Add part</button>
      </form>

      <div className="panel tickets-panel">
        <div className="table-wrapper">
          <table>
            <thead><tr><th>Part</th><th>SKU</th><th>In stock</th><th>Reorder at</th><th>Stock update</th><th>History</th></tr></thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.name}</strong><div className="subtext">{item.description || item.notes || ''}</div></td>
                  <td>{item.sku || '—'}</td>
                  <td><strong>{item.quantity}</strong> {item.unit || 'pcs'}{item.quantity <= item.reorderThreshold && <div className="subtext" style={{ color: '#a42d35' }}>Low stock</div>}</td>
                  <td>{item.reorderThreshold}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 6, minWidth: 180 }}>
                      <input aria-label={`Stock change for ${item.name}`} type="number" step="1" value={stockChanges[item.id] ?? ''} placeholder="+ / −" onChange={(e) => setStockChanges((prev) => ({ ...prev, [item.id]: e.target.value }))} />
                      <button className="btn-outline" type="button" onClick={() => adjustStock(item)}>Apply</button>
                    </div>
                  </td>
                  <td><button className="btn-outline" type="button" onClick={() => toggleHistory(item.id)}>{history[item.id] ? 'Hide' : 'View'}</button></td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan="6" className="subtext">No spare parts have been added yet.</td></tr>}
              {items.flatMap((item) => history[item.id] ? [
                <tr key={`history-${item.id}`}><td colSpan="6">
                  <strong>{item.name} stock history</strong>
                  {history[item.id].length === 0 ? <p className="subtext">No stock changes recorded.</p> : (
                    <div className="table-wrapper"><table><thead><tr><th>When</th><th>Change</th><th>Type</th><th>By</th><th>Note / ticket</th></tr></thead><tbody>
                      {history[item.id].map((entry) => <tr key={entry.id}><td>{new Date(entry.createdAt).toLocaleString()}</td><td>{entry.quantityChange > 0 ? '+' : ''}{entry.quantityChange}</td><td>{entry.type}</td><td>{entry.performedBy}</td><td>{entry.note || '—'}{entry.ticketId ? ` · Ticket #${entry.ticketId}` : ''}</td></tr>)}
                    </tbody></table></div>
                  )}
                </td></tr>,
              ] : [])}
            </tbody>
          </table>
        </div>
      </div>
      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}
