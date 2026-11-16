import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Giới hạn route theo role nền tảng HOẶC grant payroll (anyGrant).
 * `requireEmployee`: route cần hồ sơ nhân viên (Admin kỹ thuật không có hồ sơ nên backend trả 403).
 * Đây chỉ là lớp UX; backend vẫn kiểm tra quyền ở mọi API.
 */
export function RoleRoute({ allowedRoles = [], anyGrant = false, requireEmployee = false, children }) {
  const { role, user, hasAnyGrant } = useAuth();

  const restricted = allowedRoles.length > 0 || anyGrant;
  const allowed = !restricted || allowedRoles.includes(role) || (anyGrant && hasAnyGrant);
  const employeeOk = !requireEmployee || Boolean(user?.employeeId);

  if (!allowed || !employeeOk) {
    return <Navigate to="/dashboard" replace />;
  }

  return children ? children : <Outlet />;
}

export default RoleRoute;
