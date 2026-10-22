import { createContext, useContext, useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { api, tokenStore } from './api';
import type { Account,Role } from './types';
import type { ReactNode } from 'react';
interface AuthState {user:Account|null;loading:boolean;error:string;login:(email:string,password:string)=>Promise<void>;logout:()=>Promise<void>;retry:()=>void}
const Context = createContext<AuthState|null>(null);
export const useAuth = () => {const value=useContext(Context);if(!value)throw new Error('AuthProvider is required');return value;};
export const useRequiredAuth = () => {const auth=useAuth();if(!auth.user)throw new Error('Authenticated route is required');return {...auth,user:auth.user};};
export function AuthProvider({ children }: {children:ReactNode}) {
  const [user, setUser] = useState<Account|null>(null), [loading, setLoading] = useState(true), [error, setError] = useState(''), [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    const expired = () => { setUser(null); setError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'); };
    window.addEventListener('vetclinic:unauthorized', expired);
    if (!tokenStore.get()) setLoading(false);
    else { setLoading(true); setError(''); api<Account>('/auth/me', { signal: controller.signal }).then(setUser).catch(e => { if (e.name !== 'AbortError') setError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); }); }
    return () => { controller.abort(); window.removeEventListener('vetclinic:unauthorized', expired); };
  }, [attempt]);
  async function login(email:string, password:string) {
    const result = await api<{accessToken:string}>('/auth/login', { method: 'POST', body: { email, password }, token: null });
    const account = await api<Account>('/auth/me', { token: result.accessToken });
    tokenStore.set(result.accessToken); setUser(account); setError('');
  }
  async function logout() {
    try { if(tokenStore.get())await api<void>('/auth/logout',{method:'POST'}); }
    catch { setError('Đã thoát trên thiết bị. Không thể xác nhận thu hồi token với máy chủ.'); }
    finally { tokenStore.clear();setUser(null); }
  }
  return <Context.Provider value={{ user, loading, error, login, logout, retry: () => setAttempt(a => a + 1) }}>{children}</Context.Provider>;
}
export function RequireRole({ roles = ['ADMIN', 'VET', 'RECEPTIONIST'] }: {roles?:Role[]}) {
  const auth = useAuth(), location = useLocation();
  if (auth.loading) return <div className="center" role="status">Đang kiểm tra phiên đăng nhập…</div>;
  if (!auth.user && tokenStore.get()) return <div className="center"><p role="alert">{auth.error}</p><button onClick={auth.retry}>Thử lại</button> <button onClick={auth.logout}>Về đăng nhập</button></div>;
  if (!auth.user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!roles.includes(auth.user.role)) return <div className="center" role="alert">403 — Bạn không có quyền truy cập trang này.</div>;
  return <Outlet />;
}
