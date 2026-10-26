import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import './LoginPage.css';

export default function LogoutPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await logout();
      if (!cancelled) navigate('/login', { replace: true });
    })();
    return () => { cancelled = true; };
  }, [logout, navigate]);

  return (
    <div className="login-shell">
      <div className="login-card">
        <h1>Signing out</h1>
        <p>Your session is being closed.</p>
      </div>
    </div>
  );
}
