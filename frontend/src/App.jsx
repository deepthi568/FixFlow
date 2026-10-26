import { Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import TicketsPage from './pages/TicketsPage.jsx';
import TicketDetailPage from './pages/TicketDetailPage.jsx';
import AnalyticsPage from './pages/AnalyticsPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import LogoutPage from './pages/LogoutPage.jsx';
import UsersPage from './pages/UsersPage.jsx';
import InventoryPage from './pages/InventoryPage.jsx';
import TechniciansPage from './pages/TechniciansPage.jsx';
import AuditEventsPage from './pages/AuditEventsPage.jsx';
import ForbiddenPage from './pages/ForbiddenPage.jsx';
import { RequireAuth, RequireRole } from './components/RequireAuth.jsx';
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import './App.css';

function AppLayout() {
  const { user } = useAuth();
  return (
    <div className="app">
      <Sidebar />
      <main className="main">
        <header className="workspace-topbar">
          <div className="workspace-context"><span className="workspace-mark">F</span><div><strong>FixFlow Workspace</strong><span>Repair operations</span></div></div>
          <div className="workspace-user"><span>{user?.fullName || user?.username}</span><span className="workspace-role">{user?.role?.replace('_', ' ')}</span></div>
        </header>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/tickets" element={<TicketsPage />} />
          <Route path="/tickets/:id" element={<TicketDetailPage />} />
          <Route
            path="/analytics"
            element={
              <RequireRole roles={['ADMIN']}>
                <AnalyticsPage />
              </RequireRole>
            }
          />
          <Route
            path="/users"
            element={
              <RequireRole roles={['ADMIN']}>
                <UsersPage />
              </RequireRole>
            }
          />
          <Route
            path="/inventory"
            element={
              <RequireRole roles={['ADMIN', 'SERVICE_STAFF']}>
                <InventoryPage />
              </RequireRole>
            }
          />
          <Route path="/technicians" element={<RequireRole roles={['ADMIN']}><TechniciansPage /></RequireRole>} />
          <Route path="/audit" element={<RequireRole roles={['ADMIN']}><AuditEventsPage /></RequireRole>} />
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<DashboardPage />} />
        </Routes>
        <footer>FixFlow · Repair Lifecycle Management · {new Date().getFullYear()}</footer>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/logout" element={<LogoutPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/*" element={<AppLayout />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </AuthProvider>
  );
}
