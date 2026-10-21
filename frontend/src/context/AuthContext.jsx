
import React, {
  createContext,
  useState,
  useEffect,
  useCallback,
} from 'react';
import authApi from '../api/authApi';
import { STORAGE_KEYS, ROLES } from '../utils/constants';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(
    () => localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN) || null
  );
  const [isInitializing, setIsInitializing] = useState(true);

  const clearSession = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    setToken(null);
    setUser(null);
  }, []);

  // Xác minh token với backend mỗi khi ứng dụng khởi động.
  useEffect(() => {
    let cancelled = false;

    const initializeSession = async () => {
      const savedToken = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);

      if (!savedToken) {
        if (!cancelled) {
          setUser(null);
          setToken(null);
          setIsInitializing(false);
        }
        return;
      }

      try {
        const currentUser = await authApi.getCurrentUser();

        if (cancelled) return;

        if (!currentUser?.email || !currentUser?.role) {
          throw new Error('Thông tin người dùng từ máy chủ không hợp lệ.');
        }

        const normalizedRole = String(currentUser.role)
          .replace(/^ROLE_/, '')
          .toUpperCase();

        const loggedUser = {
          id: currentUser.id ?? null,
          email: currentUser.email,
          role: normalizedRole,
          fullName: currentUser.fullName || currentUser.email,
          employeeId: currentUser.employeeId ?? null,
          avatar: null,
        };

        setToken(savedToken);
        setUser(loggedUser);
        localStorage.setItem(
          STORAGE_KEYS.CURRENT_USER,
          JSON.stringify(loggedUser)
        );
      } catch {
        if (!cancelled) {
          clearSession();
        }
      } finally {
        if (!cancelled) {
          setIsInitializing(false);
        }
      }
    };

    initializeSession();

    return () => {
      cancelled = true;
    };
  }, [clearSession]);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });

    if (!res?.token || !res?.user?.email || !res?.user?.role) {
      throw new Error('Phản hồi đăng nhập từ máy chủ không hợp lệ.');
    }

    const normalizedRole = String(res.user.role)
      .replace(/^ROLE_/, '')
      .toUpperCase();

    const loggedUser = {
      id: res.user.id ?? null,
      email: res.user.email,
      role: normalizedRole,
      fullName: res.user.fullName || res.user.email,
      employeeId: res.user.employeeId ?? null,
      avatar: null,
    };

    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, res.token);
    localStorage.setItem(
      STORAGE_KEYS.CURRENT_USER,
      JSON.stringify(loggedUser)
    );

    setToken(res.token);
    setUser(loggedUser);

    return loggedUser;
  };

  const logout = () => {
    // Backend hiện không nhất thiết có endpoint logout.
    // Xóa token phía client để kết thúc phiên trên trình duyệt.
    clearSession();
  };

  // Các hàm tương thích với giao diện demo cũ.
  // Không dùng chúng để xác thực hoặc cấp quyền thật.
  const switchRole = (newRole) => {
    console.warn('switchRole chỉ dành cho demo, không thay đổi quyền backend.');
  };

  const switchUser = (userId) => {
    console.warn('switchUser chỉ dành cho demo, không thay đổi người dùng backend.');
  };

  const role = user?.role || null;

  const value = {
    user,
    token,
    role,
    login,
    logout,
    switchRole,
    switchUser,
    defaultUsers: [],
    isInitializing,
    isAuthenticated: Boolean(user && token),
    isAdmin: role === ROLES.ADMIN,
    isHR: role === ROLES.HR,
    isManager: role === ROLES.MANAGER,
    isEmployee: role === ROLES.EMPLOYEE,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export default AuthContext;
