import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import RoleRoute from './RoleRoute';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';

import LoginPage from '../features/auth/LoginPage';
import ChangePasswordPage from '../features/auth/ChangePasswordPage';
import DashboardPage from '../features/dashboard/DashboardPage';
import EmployeeListPage from '../features/employees/EmployeeListPage';
import EmployeeDetailPage from '../features/employees/EmployeeDetailPage';
import EmployeeFormPage from '../features/employees/EmployeeFormPage';
import DepartmentListPage from '../features/departments/DepartmentListPage';
import ContractListPage from '../features/contracts/ContractListPage';
import AttendancePage from '../features/attendance/AttendancePage';
import LeaveRequestPage from '../features/leave/LeaveRequestPage';
import LeaveApprovalPage from '../features/leave/LeaveApprovalPage';
import OvertimeSchedulePage from '../features/overtime/OvertimeSchedulePage';
import OvertimeRegistrationPage from '../features/overtime/OvertimeRegistrationPage';
import OvertimeApprovalPage from '../features/overtime/OvertimeApprovalPage';
import PayrollListPage from '../features/payroll/PayrollListPage';
import PayrollDetailPage from '../features/payroll/PayrollDetailPage';
import PayslipPage from '../features/payslip/PayslipPage';

import { ROLES } from '../utils/constants';

const { ADMIN, HR, MANAGER, EMPLOYEE } = ROLES;

// Lớp UX: ẩn trang không có quyền. Backend vẫn là nơi kiểm quyền thật sự (401/403/404).
const guard = (element, props) => <RoleRoute {...props}>{element}</RoleRoute>;

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/change-password" element={<ChangePasswordPage />} />

          {/* Nhân sự: HR toàn công ty, Manager team, Admin chỉ định danh tối thiểu */}
          <Route path="/employees" element={guard(<EmployeeListPage />, { allowedRoles: [ADMIN, HR, MANAGER] })} />
          <Route path="/employees/create" element={guard(<EmployeeFormPage />, { allowedRoles: [HR] })} />
          <Route path="/employees/:id/edit" element={guard(<EmployeeFormPage />, { allowedRoles: [HR] })} />
          <Route path="/employees/:id" element={guard(<EmployeeDetailPage />, { allowedRoles: [ADMIN, HR, MANAGER] })} />
          <Route path="/profile" element={guard(<EmployeeDetailPage self />, { requireEmployee: true })} />

          <Route path="/departments" element={guard(<DepartmentListPage />, { allowedRoles: [ADMIN, HR, MANAGER] })} />

          {/* Hợp đồng: HR toàn bộ; nhân viên/quản lý chỉ xem của mình; Admin không truy cập */}
          <Route path="/contracts" element={guard(<ContractListPage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />

          <Route path="/attendance" element={guard(<AttendancePage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />

          <Route path="/leave" element={guard(<LeaveRequestPage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />
          <Route path="/leave/approval" element={guard(<LeaveApprovalPage />, { allowedRoles: [HR, MANAGER], requireEmployee: true })} />

          <Route path="/overtime" element={guard(<OvertimeSchedulePage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />
          <Route path="/overtime/registration" element={guard(<OvertimeRegistrationPage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />
          <Route path="/overtime/approval" element={guard(<OvertimeApprovalPage />, { allowedRoles: [HR, MANAGER], requireEmployee: true })} />

          {/* Payroll: theo grant VIEW/PREPARE/APPROVE/LOCK, không theo role */}
          <Route path="/payroll" element={guard(<PayrollListPage />, { anyGrant: true })} />
          <Route path="/payroll/:id" element={guard(<PayrollDetailPage />, { anyGrant: true })} />

          <Route path="/payslip" element={guard(<PayslipPage />, { allowedRoles: [HR, MANAGER, EMPLOYEE], requireEmployee: true })} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default AppRoutes;
