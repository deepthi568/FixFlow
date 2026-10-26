import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export function RequireAuth() {
  const { isAuthenticated, ready } = useAuth();
  const location = useLocation();
  if (!ready) return null;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

export function RequireRole({ roles, children }) {
  const { hasRole, ready } = useAuth();
  if (!ready) return null;
  if (!hasRole(...roles)) {
    return <Navigate to="/forbidden" replace />;
  }
  return children;
}
