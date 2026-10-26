import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge.jsx';
import CreateTicketModal from '../components/CreateTicketModal.jsx';
import Toast from '../components/Toast.jsx';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import './DashboardPage.css';

const ACTIVE_STATUSES = new Set(['RECEIVED', 'DIAGNOSED', 'IN_REPAIR', 'QUALITY_CHECK', 'READY', 'ON_HOLD']);
const STATUS_ORDER = ['RECEIVED', 'DIAGNOSED', 'IN_REPAIR', 'QUALITY_CHECK', 'READY', 'DELIVERED', 'ON_HOLD', 'CANCELLED'];

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const canCreate = hasRole('ADMIN', 'SERVICE_STAFF');
  const canSeeInventory = hasRole('ADMIN', 'SERVICE_STAFF');
  const canSeeTeam = hasRole('ADMIN', 'SERVICE_STAFF');
  const [stats, setStats] = useState(null);
  const [slaAlerts, setSlaAlerts] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(false);
  const [toast, setToast] = useState({ msg: '', type: 'success' });

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const requests = [api.get('/api/tickets/stats'), api.get('/api/tickets'), api.get('/api/tickets/sla-alerts')];
      if (canSeeInventory) requests.push(api.get('/api/inventory/low-stock'));
      if (canSeeTeam) requests.push(api.get('/api/technicians'));
      const results = await Promise.all(requests);
      setStats(results[0].data);
      setTickets(results[1].data);
      setSlaAlerts(results[2].data);
      let resultIndex = 3;
      if (canSeeInventory) setLowStock(results[resultIndex++].data);
      else setLowStock([]);
      if (canSeeTeam) setTechnicians(results[resultIndex].data);
      else setTechnicians([]);
    } catch (err) {
      setError(err.response?.data?.message || 'Dashboard data could not be loaded. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [canSeeInventory, canSeeTeam]);

  useEffect(() => { loadData(); }, [loadData]);

  const overdueCount = slaAlerts?.overdue?.length ?? 0;
  const inRepairCount = stats?.byStatus?.IN_REPAIR ?? 0;
  const completedCount = stats?.byStatus?.DELIVERED ?? stats?.delivered ?? 0;

  const recentActivity = useMemo(() => [...tickets]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 6), [tickets]);

  const workload = useMemo(() => {
    const summary = new Map(technicians.map((tech) => [tech.id, {
      id: tech.id, name: tech.name, total: 0, active: 0,
    }]));
    for (const ticket of tickets) {
      if (!ticket.technician?.id) continue;
      const entry = summary.get(ticket.technician.id) || {
        id: ticket.technician.id, name: ticket.technician.name, total: 0, active: 0,
      };
      entry.total += 1;
      if (ACTIVE_STATUSES.has(ticket.status)) entry.active += 1;
      summary.set(entry.id, entry);
    }
    return [...summary.values()].sort((a, b) => b.active - a.active || a.name.localeCompare(b.name));
  }, [technicians, tickets]);

  function handleModalSuccess(message) {
    setToast({ msg: message, type: 'success' });
    loadData();
  }

  return (
    <div className="page dashboard-page">
      <header className="topbar">
        <div><p className="eyebrow">SERVICE OPERATIONS</p><h1>Good day, {user?.fullName?.split(' ')[0] || user?.username}</h1><p className="page-subtitle">Here’s the latest across your repair workflow.</p></div>
        <div className="topbar-right">
          <button className="btn-outline" onClick={loadData} disabled={loading} aria-label="Refresh dashboard">{loading ? 'Refreshing…' : 'Refresh'}</button>
          {canCreate && <button className="btn-primary" onClick={() => setModal(true)}>＋ New ticket</button>}
        </div>
      </header>

      {error && <div className="dashboard-error" role="alert"><span>{error}</span><button className="btn-outline" onClick={loadData}>Try again</button></div>}

      <section className="dashboard-metrics" aria-label="Ticket overview">
        <Metric label="Total tickets" value={loading ? null : stats?.total} detail="All active records" to="/tickets" />
        <Metric label="Open tickets" value={loading ? null : stats?.open} detail="Awaiting completion" to="/tickets" accent="amber" />
        <Metric label="In repair" value={loading ? null : inRepairCount} detail="Work in progress" to="/tickets?status=IN_REPAIR" accent="blue" />
        <Metric label="Completed" value={loading ? null : completedCount} detail="Delivered to customers" to="/tickets?status=DELIVERED" accent="green" />
        <Metric label="Overdue" value={loading ? null : overdueCount} detail="Past target date" to="/tickets?overdue=true" accent="red" />
      </section>

      <section className="panel status-distribution">
        <div className="panel-heading"><div><p className="eyebrow">CURRENT PIPELINE</p><h2>Ticket status distribution</h2></div>{hasRole('ADMIN') && <Link to="/analytics" className="view-link">View reports →</Link>}</div>
        {loading ? <LoadingRows count={3} /> : !stats ? <p className="subtext">Status counts are unavailable.</p> : (
          <div className="status-distribution-grid">{STATUS_ORDER.map((status) => {
            const count = stats.byStatus?.[status] ?? 0;
            const percent = stats.total ? Math.round((count / stats.total) * 100) : 0;
            return <div className="distribution-row" key={status}>
              <div><StatusBadge status={status} /><strong>{count}</strong></div>
              <span className="distribution-track"><i style={{ width: `${percent}%` }} /></span>
            </div>;
          })}</div>
        )}
      </section>

      <section className="panel sla-alert-section">
        <div className="panel-heading"><div><p className="eyebrow">SERVICE LEVELS</p><h2>Deadline alerts</h2><p className="sla-threshold">Approaching deadline window: {slaAlerts?.approachingDays ?? '—'} day(s)</p></div><Link to="/tickets?overdue=true" className="view-link">Review overdue →</Link></div>
        {loading ? <LoadingRows count={2} /> : !slaAlerts ? <p className="subtext">SLA alerts are unavailable.</p> : (() => {
          const items = [
            ...slaAlerts.overdue.map((item) => ({ ...item, alertType: 'Overdue' })),
            ...slaAlerts.approaching.map((item) => ({ ...item, alertType: 'Due soon' })),
          ];
          return items.length === 0 ? <div className="inventory-clear"><span aria-hidden="true">✓</span><div><strong>No deadline alerts</strong><p>There are no overdue or approaching tickets.</p></div></div> : (
            <div className="sla-alert-list">{items.slice(0, 8).map((item) => <Link to={`/tickets/${item.ticketId}`} className={`sla-alert-row ${item.alertType === 'Overdue' ? 'sla-overdue' : ''}`} key={`${item.alertType}-${item.ticketId}`}>
              <span className="sla-alert-label">{item.alertType}</span><span className="sla-ticket"><strong>Ticket #{item.ticketId}</strong><span>{item.deviceName} · {item.technicianName || 'Unassigned'}</span></span><span className="sla-date">Due {new Date(`${item.targetCompletionDate}T00:00:00`).toLocaleDateString()}</span>
            </Link>)}</div>
          );
        })()}
      </section>

      <div className="dashboard-columns">
        <section className="panel dashboard-section">
          <div className="panel-heading"><div><p className="eyebrow">LATEST UPDATES</p><h2>Recent ticket activity</h2></div><Link to="/tickets" className="view-link">All tickets →</Link></div>
          {loading ? <LoadingRows count={4} /> : recentActivity.length === 0 ? (
            <div className="empty-state"><h3>No ticket activity yet</h3><p>New and updated repair tickets will appear here.</p></div>
          ) : (
            <div className="activity-list">
              {recentActivity.map((ticket) => <Link className="activity-row" to={`/tickets/${ticket.id}`} key={ticket.id}>
                <span className="activity-icon" aria-hidden="true">{ticket.status === 'DELIVERED' ? '✓' : '↗'}</span>
                <span className="activity-main"><strong>Ticket #{ticket.id} · {ticket.customerName}</strong><span>{ticket.deviceName} · {ticket.technician?.name || 'Unassigned'}</span></span>
                <span className="activity-side"><StatusBadge status={ticket.status} /><time>{ticket.updatedAt ? new Date(ticket.updatedAt).toLocaleDateString() : ticket.createdAt ? new Date(ticket.createdAt).toLocaleDateString() : '—'}</time></span>
              </Link>)}
            </div>
          )}
        </section>

        <section className="panel dashboard-section">
          <div className="panel-heading"><div><p className="eyebrow">TEAM CAPACITY</p><h2>Technician workload</h2></div>{canSeeTeam && <Link to="/technicians" className="view-link">Manage team →</Link>}</div>
          {loading ? <LoadingRows count={3} /> : workload.length === 0 ? (
            <div className="empty-state"><h3>No assigned tickets</h3><p>Assignments will appear here when tickets have a technician.</p></div>
          ) : (
            <div className="workload-list">{workload.slice(0, 6).map((tech) => {
              const ratio = tech.total ? Math.min(100, Math.round((tech.active / tech.total) * 100)) : 0;
              return <div className="workload-row" key={tech.id}>
                <div className="workload-title"><strong>{tech.name}</strong><span>{tech.active} open · {tech.total} total</span></div>
                <div className="workload-track" aria-label={`${tech.active} active of ${tech.total} assigned`}><span style={{ width: `${ratio}%` }} /></div>
              </div>;
            })}</div>
          )}
        </section>

        {canSeeInventory && <section className="panel dashboard-section low-stock-section">
          <div className="panel-heading"><div><p className="eyebrow">INVENTORY</p><h2>Low-stock alerts</h2></div><Link to="/inventory" className="view-link">Open inventory →</Link></div>
          {loading ? <LoadingRows count={2} /> : lowStock.length === 0 ? (
            <div className="inventory-clear"><span aria-hidden="true">✓</span><div><strong>Stock levels look good</strong><p>No parts are at or below their reorder threshold.</p></div></div>
          ) : (
            <div className="low-stock-list">{lowStock.slice(0, 5).map((item) => <div className="low-stock-row" key={item.id}>
              <div><strong>{item.name}</strong><span>{item.sku || 'No SKU'} · reorder at {item.reorderThreshold}</span></div>
              <strong className="stock-count">{item.quantity} left</strong>
            </div>)}</div>
          )}
        </section>}
      </div>

      {canCreate && <CreateTicketModal open={modal} onClose={() => setModal(false)} onSuccess={handleModalSuccess} />}
      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}

function Metric({ label, value, detail, to, accent = 'default' }) {
  return <Link className={`metric-card metric-${accent}`} to={to}>
    <span className="metric-label">{label}</span>
    <strong className={value == null ? 'metric-value is-loading' : 'metric-value'}>{value == null ? '—' : value}</strong>
    <span className="metric-detail">{detail}</span>
  </Link>;
}

function LoadingRows({ count }) {
  return <div className="dashboard-loading" aria-label="Loading data">{Array.from({ length: count }, (_, i) => <div className="skeleton-row" key={i} />)}</div>;
}
