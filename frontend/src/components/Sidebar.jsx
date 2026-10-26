import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import './Sidebar.css';

const ROLE_LABEL = {
  ADMIN: 'Administrator',
  SERVICE_STAFF: 'Service staff',
  TECHNICIAN: 'Technician',
};

export default function Sidebar() {
  const { user, hasRole } = useAuth();
  const initials = (user?.fullName || user?.username || 'FF').slice(0, 2).toUpperCase();

  const nav = [
    { to: '/', icon: '▦', label: 'Dashboard' },
    { to: '/tickets', icon: '▤', label: 'Repair Tickets' },
  ];
  if (hasRole('ADMIN')) {
    nav.push({ to: '/audit', icon: '◷', label: 'Audit trail' });
    nav.push({ to: '/analytics', icon: '◈', label: 'Analytics' });
    nav.push({ to: '/users', icon: '◉', label: 'Users' });
    nav.push({ to: '/technicians', icon: '⚒', label: 'Technicians' });
  }
  if (hasRole('ADMIN', 'SERVICE_STAFF')) nav.push({ to: '/inventory', icon: '▣', label: 'Inventory' });

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-icon">F</div>
        <div>
          <h2>FixFlow</h2>
          <p>REPAIR MANAGEMENT</p>
        </div>
      </div>

      <p className="nav-label">WORKSPACE</p>
      <nav>
        {nav.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            {icon}&nbsp;&nbsp;{label}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="avatar">{initials}</div>
        <div>
          <strong>{user?.fullName || user?.username}</strong>
          <p>{ROLE_LABEL[user?.role] || user?.role}</p>
          <NavLink to="/logout" className="logout-link">Sign out</NavLink>
        </div>
      </div>
    </aside>
  );
}
