import './StatusBadge.css';

const STATUS_META = {
  RECEIVED:      { label: 'Received',       cls: 'badge-received' },
  DIAGNOSED:     { label: 'Diagnosed',      cls: 'badge-diagnosed' },
  IN_REPAIR:     { label: 'In Repair',      cls: 'badge-in-repair' },
  QUALITY_CHECK: { label: 'Quality Check',  cls: 'badge-quality' },
  READY:         { label: 'Ready',          cls: 'badge-ready' },
  DELIVERED:     { label: 'Delivered',      cls: 'badge-delivered' },
  ON_HOLD:       { label: 'On Hold',        cls: 'badge-on-hold' },
  CANCELLED:     { label: 'Cancelled',      cls: 'badge-cancelled' },
};

export default function StatusBadge({ status }) {
  const meta = STATUS_META[status] ?? { label: status, cls: 'badge-default' };
  return <span className={`status-badge ${meta.cls}`}>{meta.label}</span>;
}
