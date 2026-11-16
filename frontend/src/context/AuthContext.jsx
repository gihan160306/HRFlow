import React, { createContext, useState, useEffect, useCallback, useMemo } from 'react';
import authApi from '../api/authApi';
import { setUnauthorizedHandler } from '../api/httpClient';
import { STORAGE_KEYS, ROLES } from '../utils/constants';

export const AuthContext = createContext(null);

/** Chuẩn hóa payload `user` của backend (/auth/login, /auth/me, /auth/change-password). */
function normalizeUser(u, mustChangePassword) {
  return {
    id: u.accountId ?? null,
    email: u.email,
    role: String(u.role || '').toUpperCase(),
    fullName: u.fullName || u.email,
    employeeId: u.employeeId ?? null,
    employeeCode: u.employeeCode ?? null,
    departmentName: u.departmentName ?? null,
    jobTitleName: u.jobTitleName ?? null,
    grants: Array.isArray(u.grants) ? u.grants : [],
    teamSize: u.teamSize ?? 0,
    approver: Boolean(u.approver),
    mustChangePassword: Boolean(mustChangePassword ?? u.mustChangePassword),
  };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN) || null);
  const [isInitializing, setIsInitializing] = useState(true);

  const clearSession = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    setToken(null);
    setUser(null);
  }, []);

  // Token hết hạn / bị thu hồi -> backend trả 401 -> quay về trang đăng nhập.
  useEffect(() => {
    setUnauthorizedHandler(clearSession);
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  // Xác minh token với backend mỗi khi ứng dụng khởi động.
  useEffect(() => {
    let cancelled = false;

    const init = async () => {
      const saved = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
      if (!saved) {
        setIsInitializing(false);
        return;
      }
      try {
        const me = await authApi.me();
        if (cancelled) return;
        if (!me?.email || !me?.role) throw new Error('invalid');
        setToken(saved);
        setUser(normalizeUser(me));
      } catch {
        if (!cancelled) clearSession();
      } finally {
        if (!cancelled) setIsInitializing(false);
      }
    };

    init();
    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  const applyTokenResponse = useCallback((res) => {
    if (!res?.token || !res?.user?.email || !res?.user?.role) {
      throw new Error('Phản hồi từ máy chủ không hợp lệ.');
    }
    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, res.token);
    const loggedUser = normalizeUser(res.user, res.mustChangePassword);
    setToken(res.token);
    setUser(loggedUser);
    return loggedUser;
  }, []);

  const login = useCallback(
    async (email, password) => applyTokenResponse(await authApi.login(email, password)),
    [applyTokenResponse]
  );

  const changePassword = useCallback(
    async (currentPassword, newPassword) =>
      applyTokenResponse(await authApi.changePassword(currentPassword, newPassword)),
    [applyTokenResponse]
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout(); // thu hồi token phía server (token_version + 1)
    } catch {
      // token có thể đã hết hạn - vẫn xóa phiên cục bộ
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo(() => {
    const role = user?.role || null;
    const grants = user?.grants || [];
    return {
      user,
      token,
      role,
      grants,
      login,
      logout,
      changePassword,
      isInitializing,
      isAuthenticated: Boolean(user && token),
      mustChangePassword: Boolean(user?.mustChangePassword),
      hasGrant: (g) => grants.includes(g),
      hasAnyGrant: grants.length > 0,
      isAdmin: role === ROLES.ADMIN,
      isHR: role === ROLES.HR,
      isManager: role === ROLES.MANAGER,
      isEmployee: role === ROLES.EMPLOYEE,
    };
  }, [user, token, login, logout, changePassword, isInitializing]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthContext;
