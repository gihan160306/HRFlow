import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import RoleRoute from './RoleRoute';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';

// Features
import LoginPage from '../features/auth/LoginPage';
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

export function AppRoutes() {
  return (
    <Routes>
      {/* Auth routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Protected App routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Employees */}
          <Route
            path="/employees"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]}>
                <EmployeeListPage />
              </RoleRoute>
            }
          />
          <Route
            path="/employees/create"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR]}>
                <EmployeeFormPage />
              </RoleRoute>
            }
          />
          <Route
            path="/employees/:id"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]}>
                <EmployeeDetailPage />
              </RoleRoute>
            }
          />

          {/* Departments */}
          <Route
            path="/departments"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR]}>
                <DepartmentListPage />
              </RoleRoute>
            }
          />

          {/* Contracts */}
          <Route
            path="/contracts"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR]}>
                <ContractListPage />
              </RoleRoute>
            }
          />

          {/* Attendance */}
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/attendance/history" element={<AttendancePage />} />

          {/* Leave */}
          <Route path="/leave" element={<LeaveRequestPage />} />
          <Route path="/leave/request" element={<LeaveRequestPage />} />
          <Route
            path="/leave/approval"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]}>
                <LeaveApprovalPage />
              </RoleRoute>
            }
          />

          {/* Overtime */}
          <Route path="/overtime" element={<OvertimeSchedulePage />} />
          <Route path="/overtime/schedule" element={<OvertimeSchedulePage />} />
          <Route path="/overtime/registration" element={<OvertimeRegistrationPage />} />
          <Route
            path="/overtime/approval"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]}>
                <OvertimeApprovalPage />
              </RoleRoute>
            }
          />

          {/* Payroll */}
          <Route
            path="/payroll"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR]}>
                <PayrollListPage />
              </RoleRoute>
            }
          />
          <Route
            path="/payroll/:id"
            element={
              <RoleRoute allowedRoles={[ROLES.ADMIN, ROLES.HR]}>
                <PayrollDetailPage />
              </RoleRoute>
            }
          />

          {/* Payslip */}
          <Route path="/payslip" element={<PayslipPage />} />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default AppRoutes;
