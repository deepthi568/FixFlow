import { useEffect, useState, useCallback, useMemo } from 'react';
import StatusBadge from '../components/StatusBadge.jsx';
import { api } from '../api/client.js';
import './AnalyticsPage.css';

const STATUS_ORDER = [
  'RECEIVED','DIAGNOSED','IN_REPAIR','QUALITY_CHECK',
  'READY','DELIVERED','ON_HOLD','CANCELLED',
];

/** Hue per status for the SVG bars */
const STATUS_COLORS = {
  RECEIVED:      '#4e9de8',
  DIAGNOSED:     '#8b5cf6',
  IN_REPAIR:     '#f59e0b',
  QUALITY_CHECK: '#f97316',
  READY:         '#14b8a6',
  DELIVERED:     '#22c55e',
  ON_HOLD:       '#94a3b8',
  CANCELLED:     '#ef4444',
};

/** SVG bar chart – pure SVG, zero libraries. */
function BarChart({ byStatus }) {
  const BAR_W  = 48;
  const GAP    = 22;
  const PAD_L  = 44;
  const PAD_B  = 60;
  const PAD_T  = 16;
  const CHART_H = 200;

  const counts  = STATUS_ORDER.map(s => byStatus[s] ?? 0);
  const maxVal  = Math.max(...counts, 1);
  const totalW  = PAD_L + STATUS_ORDER.length * (BAR_W + GAP);
  const svgH    = CHART_H + PAD_B + PAD_T;

  // Y-axis grid lines (5 steps)
  const steps = 5;
  const gridLines = Array.from({ length: steps + 1 }, (_, i) => {
    const value = Math.round((maxVal / steps) * (steps - i));
    const y = PAD_T + (CHART_H * i) / steps;
    return { value, y };
  });

  return (
    <svg
      viewBox={`0 0 ${totalW} ${svgH}`}
      aria-label="Ticket counts by status"
      role="img"
      className="bar-chart-svg"
    >
      {/* Grid lines */}
      {gridLines.map(({ value, y }) => (
        <g key={y}>
          <line
            x1={PAD_L} y1={y} x2={totalW - 8} y2={y}
            stroke="#edf0f4" strokeWidth="1"
          />
          <text
            x={PAD_L - 6} y={y + 4}
            textAnchor="end"
            fontSize="10"
            fill="#9aabb8"
          >
            {value}
          </text>
        </g>
      ))}

      {/* Bars */}
      {STATUS_ORDER.map((s, i) => {
        const count  = byStatus[s] ?? 0;
        const barH   = maxVal > 0 ? (count / maxVal) * CHART_H : 0;
        const x      = PAD_L + i * (BAR_W + GAP);
        const y      = PAD_T + CHART_H - barH;
        const lines  = s.split('_');

        return (
          <g key={s}>
            {/* Bar */}
            <rect
              x={x} y={y}
              width={BAR_W} height={barH}
              rx="5"
              fill={STATUS_COLORS[s]}
              opacity="0.88"
            />
            {/* Count above bar */}
            {count > 0 && (
              <text
                x={x + BAR_W / 2}
                y={y - 6}
                textAnchor="middle"
                fontSize="11"
                fontWeight="700"
                fill={STATUS_COLORS[s]}
              >
                {count}
              </text>
            )}
            {/* Status labels below x-axis */}
            {lines.map((line, li) => (
              <text
                key={li}
                x={x + BAR_W / 2}
                y={PAD_T + CHART_H + 14 + li * 13}
                textAnchor="middle"
                fontSize="9"
                fill="#667788"
              >
                {line}
              </text>
            ))}
          </g>
        );
      })}

      {/* Y axis line */}
      <line
        x1={PAD_L} y1={PAD_T}
        x2={PAD_L} y2={PAD_T + CHART_H}
        stroke="#dce3eb" strokeWidth="1"
      />
    </svg>
  );
}

export default function AnalyticsPage() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/api/tickets');
      setTickets(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load report data. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadTickets(); }, [loadTickets]);

  const stats = useMemo(() => {
    const filtered = tickets.filter((ticket) => {
      const created = ticket.createdAt?.slice(0, 10) || '';
      return (!fromDate || created >= fromDate) && (!toDate || created <= toDate);
    });
    const byStatus = Object.fromEntries(STATUS_ORDER.map((status) => [status, 0]));
    filtered.forEach((ticket) => { if (ticket.status in byStatus) byStatus[ticket.status] += 1; });
    const delivered = byStatus.DELIVERED;
    const cancelled = byStatus.CANCELLED;
    return { total: filtered.length, open: filtered.length - delivered - cancelled, delivered, cancelled, byStatus };
  }, [tickets, fromDate, toDate]);

  return (
    <div className="page">
      <header className="topbar">
        <div>
          <p className="eyebrow">INSIGHTS</p>
          <h1>Analytics</h1>
        </div>
        <button id="analytics-refresh-btn" className="btn-outline" onClick={loadTickets} disabled={loading}>Refresh data</button>
      </header>

      <div className="panel report-filters" aria-label="Report date filters">
        <div><p className="eyebrow">REPORT PERIOD</p><h2>Filter by ticket creation date</h2></div>
        <label>From<input type="date" value={fromDate} max={toDate || undefined} onChange={(e) => setFromDate(e.target.value)} /></label>
        <label>To<input type="date" value={toDate} min={fromDate || undefined} onChange={(e) => setToDate(e.target.value)} /></label>
        {(fromDate || toDate) && <button className="btn-outline" type="button" onClick={() => { setFromDate(''); setToDate(''); }}>Clear dates</button>}
      </div>
      {error && <div className="dashboard-error" role="alert"><span>{error}</span><button className="btn-outline" onClick={loadTickets}>Try again</button></div>}

      {/* Summary cards */}
      <section className="stats">
        {[
          { label: 'Total Tickets',  value: loading ? '—' : stats.total },
          { label: 'Open / In-Progress', value: loading ? '—' : stats.open },
          { label: 'Delivered',      value: loading ? '—' : stats.delivered },
          { label: 'Cancelled',      value: loading ? '—' : stats.cancelled },
        ].map(({ label, value }) => (
          <div key={label} className="stat-card">
            <p>{label}</p>
            <h2>{value}</h2>
          </div>
        ))}
      </section>

      {/* Bar chart */}
      <div className="panel chart-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">DISTRIBUTION</p>
            <h2>Tickets by Status</h2>
          </div>
          <p className="chart-subtitle">{fromDate || toDate ? `${fromDate || 'Beginning'} to ${toDate || 'Today'}` : 'All non-deleted tickets'}</p>
        </div>

        {loading ? (
          <div className="chart-skeleton" />
        ) : !error && stats.total > 0 ? (
          <BarChart byStatus={stats.byStatus} />
        ) : !error ? (
          <div className="empty-state"><h3>No tickets in this date range</h3><p>Adjust the dates to view another reporting period.</p></div>
        ) : (
          <div className="empty-state"><h3>No data available</h3></div>
        )}
      </div>

      {/* Status breakdown list */}
      {!loading && !error && (
        <div className="panel breakdown-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">BY STATUS</p>
              <h2>Detailed Breakdown</h2>
            </div>
          </div>
          <div className="breakdown-table">
            {STATUS_ORDER.map(s => {
              const count = stats.byStatus?.[s] ?? 0;
              const pct   = stats.total > 0 ? ((count / stats.total) * 100).toFixed(1) : '0.0';
              return (
                <div key={s} className="bd-row">
                  <StatusBadge status={s} />
                  <div className="bd-bar-track">
                    <div
                      className="bd-bar-fill"
                      style={{ width: `${pct}%`, background: STATUS_COLORS[s] }}
                    />
                  </div>
                  <span className="bd-count">{count}</span>
                  <span className="bd-pct">{pct}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

    </div>
  );
}
