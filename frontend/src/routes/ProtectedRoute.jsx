import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function ProtectedRoute({ children }) {
  const { isAuthenticated, isInitializing, mustChangePassword, isKiosk } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">
        Đang kiểm tra phiên đăng nhập...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  // Tài khoản mới/được reset phải đổi mật khẩu trước khi dùng hệ thống.
  if (mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />;
  }

  // Máy chấm công tại quầy (tài khoản KIOSK) chỉ dùng màn hình /kiosk toàn màn hình.
  if (isKiosk && !mustChangePassword && location.pathname !== '/kiosk') {
    return <Navigate to="/kiosk" replace />;
  }

  return children ?? <Outlet />;
}

export default ProtectedRoute;
