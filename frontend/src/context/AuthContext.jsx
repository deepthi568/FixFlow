import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, clearSession, hasToken, loadStoredUser, saveSession, setUnauthorizedHandler } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => loadStoredUser());
  const [ready, setReady] = useState(false);

  const logoutLocal = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logoutLocal();
      navigate('/login', { replace: true });
    });
  }, [logoutLocal, navigate]);

  useEffect(() => {
    async function hydrate() {
      if (!hasToken()) {
        setReady(true);
        return;
      }
      try {
        const { data } = await api.get('/api/auth/me');
        setUser(data);
        saveSession(sessionStorage.getItem('fixflow.token'), data);
      } catch {
        logoutLocal();
      } finally {
        setReady(true);
      }
    }
    hydrate();
  }, [logoutLocal]);

  const login = useCallback(async (username, password) => {
    const { data } = await api.post('/api/auth/login', { username, password });
    saveSession(data.token, data.user);
    setUser(data.user);
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout');
    } catch {
      // Token may already be invalid; still clear the local session.
    }
    logoutLocal();
  }, [logoutLocal]);

  const value = useMemo(() => ({
    user,
    ready,
    login,
    logout,
    isAuthenticated: Boolean(user),
    hasRole: (...roles) => Boolean(user && roles.includes(user.role)),
  }), [user, ready, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
