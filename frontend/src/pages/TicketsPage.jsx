import { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import CreateTicketModal from '../components/CreateTicketModal.jsx';
import Toast from '../components/Toast.jsx';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import './TicketsPage.css';

const STATUSES = [
  'RECEIVED','DIAGNOSED','IN_REPAIR','QUALITY_CHECK',
  'READY','DELIVERED','ON_HOLD','CANCELLED',
];

const FILTER_TABS = [
  { key: '',            label: 'All' },
  { key: 'RECEIVED',    label: 'Received' },
  { key: 'DIAGNOSED',   label: 'Diagnosed' },
  { key: 'IN_REPAIR',   label: 'In Repair' },
  { key: 'QUALITY_CHECK', label: 'QC' },
  { key: 'READY',       label: 'Ready' },
  { key: 'DELIVERED',   label: 'Delivered' },
  { key: 'ON_HOLD',     label: 'On Hold' },
  { key: 'CANCELLED',   label: 'Cancelled' },
];

const PRIORITY_LABELS = { HIGH: '🔴 High', MEDIUM: '🟡 Medium', LOW: '🟢 Low' };

export default function TicketsPage() {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canCreate = hasRole('ADMIN', 'SERVICE_STAFF');
  const canAssign = hasRole('ADMIN', 'SERVICE_STAFF');
  const canDelete = hasRole('ADMIN');
  const statusOptions = hasRole('TECHNICIAN')
    ? ['DIAGNOSED', 'IN_REPAIR', 'QUALITY_CHECK', 'ON_HOLD', 'READY']
    : STATUSES;
  const [searchParams, setSearchParams] = useSearchParams();

  const [tickets,      setTickets]   = useState([]);
  const [technicians,  setTechs]     = useState([]);
  const [search,       setSearch]    = useState('');
  const [statusFilter, setStatus]    = useState(() => searchParams.get('status') || '');
  const [priorityFilter, setPriority] = useState('');
  const [sortBy, setSortBy] = useState('updated');
  const [overdueOnly, setOverdueOnly] = useState(() => searchParams.get('overdue') === 'true');
  const [overdueIds, setOverdueIds] = useState([]);
  const [approachingIds, setApproachingIds] = useState([]);
  const [slaReady, setSlaReady] = useState(false);
  const [slaError, setSlaError] = useState('');
  const [loading,      setLoading]   = useState(true);
  const [loadError,    setLoadError] = useState('');
  const [modal,        setModal]     = useState(false);
  const [toast,        setToast]     = useState({ msg: '', type: 'success' });
  const [deleting,     setDeleting]  = useState(null);

  const searchTimerRef = useRef(null);

  const showToast = useCallback((msg, type = 'success') => setToast({ msg, type }), []);

  const loadTickets = useCallback(async (currentSearch, currentStatus, currentPriority) => {
    setLoading(true);
    setLoadError('');
    try {
      const params = {};
      if (currentSearch?.trim())   params.search   = currentSearch.trim();
      if (currentStatus)           params.status   = currentStatus;
      if (currentPriority)         params.priority = currentPriority;
      const { data } = await api.get('/api/tickets', { params });
      setTickets(data);
    } catch (err) {
      const message = err.response?.data?.message || 'Could not load tickets. Check your connection and try again.';
      setLoadError(message);
      showToast(message, 'error');
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  // Load technicians once
  useEffect(() => {
    if (!canAssign) return;
    api.get('/api/technicians').then(r => setTechs(r.data)).catch(() => setTechs([]));
  }, [canAssign]);

  const loadSlaAlerts = useCallback(async () => {
    setSlaError('');
    setSlaReady(false);
    try {
      const { data } = await api.get('/api/tickets/sla-alerts');
      setOverdueIds(data.overdue.map((item) => item.ticketId));
      setApproachingIds(data.approaching.map((item) => item.ticketId));
    } catch (err) {
      const message = err.response?.data?.message || 'Could not load SLA alerts.';
      setSlaError(message);
      showToast(message, 'error');
    } finally {
      setSlaReady(true);
    }
  }, [showToast]);

  useEffect(() => { loadSlaAlerts(); }, [loadSlaAlerts]);

  // Load tickets when filters change (debounce search)
  useEffect(() => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(
      () => loadTickets(search, statusFilter, priorityFilter),
      search ? 350 : 0,
    );
    return () => clearTimeout(searchTimerRef.current);
  }, [search, statusFilter, priorityFilter, loadTickets]);

  function refresh() {
    loadTickets(search, statusFilter, priorityFilter);
    loadSlaAlerts();
  }

  async function handleStatusChange(id, status) {
    try {
      await api.patch(`/api/tickets/${id}/status`, null, { params: { status } });
      showToast(`Ticket #${id} moved to ${status.replace(/_/g, ' ')}.`);
      refresh();
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not update status.', 'error');
    }
  }

  async function handleTechnicianChange(id, technicianId) {
    try {
      const params = technicianId ? { technicianId } : {};
      await api.patch(`/api/tickets/${id}/technician`, null, { params });
      showToast(`Ticket #${id} technician updated.`);
      refresh();
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not update technician.', 'error');
    }
  }

  async function handleDelete(id) {
    if (!window.confirm(`Archive ticket #${id}? It will be hidden but remain in the database.`)) return;
    setDeleting(id);
    try {
      await api.delete(`/api/tickets/${id}`);
      showToast(`Ticket #${id} archived.`);
      refresh();
    } catch (err) {
      showToast(err.response?.data?.message || 'Could not archive ticket.', 'error');
    } finally {
      setDeleting(null);
    }
  }

  const isFiltered = Boolean(search || statusFilter || priorityFilter || overdueOnly);
  const visibleTickets = [...tickets]
    .filter((ticket) => !overdueOnly || overdueIds.includes(ticket.id))
    .sort((a, b) => {
      if (sortBy === 'target') return (a.targetCompletionDate || '9999').localeCompare(b.targetCompletionDate || '9999');
      if (sortBy === 'priority') return ['HIGH', 'MEDIUM', 'LOW'].indexOf(a.priority) - ['HIGH', 'MEDIUM', 'LOW'].indexOf(b.priority);
      if (sortBy === 'id') return b.id - a.id;
      const aDate = new Date(sortBy === 'created' ? a.createdAt || 0 : a.updatedAt || a.createdAt || 0);
      const bDate = new Date(sortBy === 'created' ? b.createdAt || 0 : b.updatedAt || b.createdAt || 0);
      return bDate - aDate;
    });

  return (
    <div className="page">
      {/* ── Header ─────────────────────────────────────────── */}
      <header className="topbar">
        <div>
          <p className="eyebrow">SERVICE REGISTER</p>
          <h1>Repair Tickets</h1>
        </div>
        <div className="topbar-right">
          <button id="tickets-refresh-btn" className="btn-outline" onClick={refresh}>↻ Refresh</button>
          {canCreate && (
            <button id="tickets-create-btn" className="btn-primary" onClick={() => setModal(true)}>
              ＋ New Ticket
            </button>
          )}
        </div>
      </header>

      {/* ── Search + Filters ───────────────────────────────── */}
      <div className="filters-bar">
        <div className="search-wrap">
          <span className="search-icon">⌕</span>
          <input
            id="tickets-search"
            className="search-input"
            type="search"
            placeholder="Search by ID, customer, or device…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            aria-label="Search tickets"
          />
        </div>
        <select
          className="priority-select"
          value={priorityFilter}
          onChange={e => setPriority(e.target.value)}
          aria-label="Filter by priority"
        >
          <option value="">All Priorities</option>
          <option value="HIGH">🔴 High</option>
          <option value="MEDIUM">🟡 Medium</option>
          <option value="LOW">🟢 Low</option>
        </select>
        <label className="overdue-filter"><input type="checkbox" checked={overdueOnly} onChange={(e) => setOverdueOnly(e.target.checked)} /> Overdue only</label>
        <label className="sort-control">Sort
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} aria-label="Sort tickets">
            <option value="updated">Recently updated</option><option value="created">Recently created</option><option value="target">Target date</option><option value="priority">Priority</option><option value="id">Ticket number</option>
          </select>
        </label>
        {isFiltered && (
          <button
            className="btn-clear"
            onClick={() => { setSearch(''); setStatus(''); setPriority(''); setOverdueOnly(false); setSearchParams({}); }}
            aria-label="Clear all filters"
          >
            ✕ Clear
          </button>
        )}
      </div>

      {/* ── Status Tabs ────────────────────────────────────── */}
      <div className="filter-tabs" role="group" aria-label="Filter tickets by status">
        {FILTER_TABS.map(({ key, label }) => (
          <button
            key={key}
            aria-pressed={statusFilter === key}
            className={`filter-tab ${statusFilter === key ? 'active' : ''}`}
            onClick={() => {
              setStatus(key)
              setSearchParams((prev) => {
                const next = new URLSearchParams(prev)
                if (key) next.set('status', key)
                else next.delete('status')
                return next
              }, { replace: true })
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {/* ── Tickets table ──────────────────────────────────── */}
      <div className="panel tickets-panel">
        {loading || (overdueOnly && !slaReady) ? (
          <div className="skeleton-rows">
            {[1,2,3,4].map(i => <div key={i} className="skeleton-row" />)}
          </div>
        ) : overdueOnly && slaError ? (
          <div className="empty-state" role="alert"><h3>SLA alerts are unavailable</h3><p>{slaError}</p><button className="btn-outline" onClick={loadSlaAlerts}>Try again</button></div>
        ) : loadError ? (
          <div className="empty-state" role="alert"><h3>Tickets are unavailable</h3><p>{loadError}</p><button className="btn-outline" onClick={refresh}>Try again</button></div>
        ) : visibleTickets.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">▤</div>
            <h3>{isFiltered ? 'No tickets match your filter.' : 'No repair tickets yet.'}</h3>
            <p>{isFiltered ? 'Try adjusting your search or filter.' : 'Create your first ticket using the button above.'}</p>
            {isFiltered && (
              <button className="btn-outline" style={{ marginTop: 14 }}
                onClick={() => { setSearch(''); setStatus(''); setPriority(''); setOverdueOnly(false); setSearchParams({}); }}>
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          <div className="table-wrapper">
            <table id="tickets-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Customer</th>
                  <th>Device / Problem</th>
                  <th>Priority</th>
                  <th>Technician</th>
                  <th>Cost ₹</th>
                  <th>Status</th>
                  <th>Target Date</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleTickets.map(t => (
                  <tr key={t.id} className={deleting === t.id ? 'row-deleting' : ''}>
                    <td className="id-cell">#{t.id}</td>
                    <td>
                      <strong>{t.customerName}</strong>
                      {t.customerPhone && <span className="subtext">{t.customerPhone}</span>}
                      {t.customerEmail && <span className="subtext">{t.customerEmail}</span>}
                    </td>
                    <td className="device-cell">
                      <strong>{t.deviceName}</strong>
                      <span className="subtext problem-clamp">{t.problemDescription}</span>
                    </td>
                    <td>
                      <span className={`priority-badge priority-${t.priority?.toLowerCase()}`}>
                        {PRIORITY_LABELS[t.priority] ?? t.priority}
                      </span>
                    </td>
                    <td>
                      {canAssign ? (
                        <select
                          className="inline-select"
                          value={t.technician?.id ?? ''}
                          onChange={e => handleTechnicianChange(t.id, e.target.value || null)}
                          aria-label={`Technician for ticket ${t.id}`}
                        >
                          <option value="">Unassigned</option>
                          {technicians.map(tech => (
                            <option key={tech.id} value={tech.id}>{tech.name}</option>
                          ))}
                        </select>
                      ) : (
                        t.technician?.name ?? <span className="subtext">Unassigned</span>
                      )}
                    </td>
                    <td>{t.estimatedCost != null
                      ? `₹${Number(t.estimatedCost).toFixed(2)}`
                      : <span className="subtext">—</span>}
                    </td>
                    <td>
                      <select
                        className="status-select"
                        value={t.status}
                        onChange={e => handleStatusChange(t.id, e.target.value)}
                        aria-label={`Status for ticket ${t.id}`}
                      >
                        {!statusOptions.includes(t.status) && <option value={t.status} disabled>{t.status.replace(/_/g, ' ')}</option>}
                        {statusOptions.map(s => (
                          <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
                        ))}
                      </select>
                    </td>
                    <td className="subtext date-cell">
                      {t.targetCompletionDate
                        ? <>{new Date(t.targetCompletionDate + 'T00:00:00').toLocaleDateString()}{overdueIds.includes(t.id) && <span className="sla-tag sla-tag-overdue">Overdue</span>}{approachingIds.includes(t.id) && <span className="sla-tag sla-tag-soon">Due soon</span>}</>
                        : <span>—</span>}
                    </td>
                    <td className="subtext date-cell">
                      {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="actions-cell">
                      <button
                        className="btn-icon btn-view"
                        onClick={() => navigate(`/tickets/${t.id}`)}
                        aria-label={`View ticket ${t.id}`}
                        title="View / Edit"
                      >
                        ✎
                      </button>
                      {canDelete && (
                        <button
                          className="btn-delete"
                          onClick={() => handleDelete(t.id)}
                          disabled={deleting === t.id}
                          aria-label={`Archive ticket ${t.id}`}
                          title="Archive ticket"
                        >
                          🗑
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="table-footer">{visibleTickets.length} ticket{visibleTickets.length !== 1 ? 's' : ''} shown</p>
      </div>

      {canCreate && (
        <CreateTicketModal
          open={modal}
          onClose={() => setModal(false)}
          onSuccess={msg => { showToast(msg); refresh(); }}
        />
      )}

      <Toast message={toast.msg} type={toast.type} onClose={() => setToast({ msg: '', type: toast.type })} />
    </div>
  );
}
