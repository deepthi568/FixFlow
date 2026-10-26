import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import './TicketsPage.css';

export default function AuditEventsPage() {
  const [events, setEvents] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/api/audit-events');
      setEvents(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Audit history could not be loaded.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const visibleEvents = useMemo(() => {
    const needle = search.trim().toLowerCase();
    if (!needle) return events;
    return events.filter((event) => [event.actor, event.actionType, event.recordType, event.recordId, event.details]
      .some((value) => String(value ?? '').toLowerCase().includes(needle)));
  }, [events, search]);

  return (
    <div className="page">
      <header className="topbar"><div><p className="eyebrow">ADMINISTRATION</p><h1>Audit trail</h1><p className="page-subtitle">Recent business actions recorded by FixFlow.</p></div><button className="btn-outline" onClick={load} disabled={loading}>Refresh</button></header>
      <div className="filters-bar">
        <input className="search-input" type="search" aria-label="Search audit events" placeholder="Search actor, action, record…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <span className="subtext">Showing up to 200 most recent events</span>
      </div>
      <div className="panel tickets-panel">
        {loading ? <div className="skeleton-rows">{[1, 2, 3, 4].map((index) => <div className="skeleton-row" key={index} />)}</div>
          : error ? <div className="empty-state" role="alert"><h3>Audit history unavailable</h3><p>{error}</p><button className="btn-outline" onClick={load}>Try again</button></div>
            : visibleEvents.length === 0 ? <div className="empty-state"><h3>{search ? 'No matching audit events' : 'No audit events recorded'}</h3><p>{search ? 'Try a different search.' : 'Actions will appear here as records are created or updated.'}</p></div>
              : <div className="table-wrapper"><table>
                <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Record</th><th>Change details</th></tr></thead>
                <tbody>{visibleEvents.map((event) => <tr key={event.id}>
                  <td className="audit-time">{new Date(event.occurredAt).toLocaleString()}</td>
                  <td><strong>{event.actor}</strong></td>
                  <td><span className="audit-action">{event.actionType.replaceAll('_', ' ')}</span></td>
                  <td>{event.recordType} #{event.recordId}</td>
                  <td className="audit-details">{event.details || '—'}</td>
                </tr>)}</tbody>
              </table></div>}
        {!loading && !error && visibleEvents.length > 0 && <p className="table-footer">{visibleEvents.length} event{visibleEvents.length !== 1 ? 's' : ''} shown</p>}
      </div>
    </div>
  );
}
