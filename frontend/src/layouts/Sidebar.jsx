import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Building2,
  FileCheck2,
  Clock,
  CalendarDays,
  Zap,
  Calculator,
  Receipt,
  SlidersHorizontal,
  Wallet,
  BarChart3,
  ShieldCheck,
  GitBranch,
  ScrollText,
  BookOpen,
  Bell,
  X,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import logoSvg from '../assets/logo.svg';

const NAV_ITEMS = [
  { path: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
  { path: '/employees', label: 'Nhân viên', icon: Users, roles: ['ADMIN', 'HR', 'MANAGER'] },
  { path: '/departments', label: 'Phòng ban', icon: Building2, roles: ['ADMIN', 'HR', 'MANAGER'] },
  { path: '/contracts', label: 'Hợp đồng', icon: FileCheck2, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  { path: '/attendance', label: 'Chấm công', icon: Clock, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  { path: '/leave', label: 'Nghỉ phép', icon: CalendarDays, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  { path: '/overtime', label: 'Tăng ca (OT)', icon: Zap, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  // Payroll: theo grant (VIEW/PREPARE/APPROVE/LOCK), không theo role.
  { path: '/payroll', label: 'Kỳ lương', icon: Calculator, grant: true },
  { path: '/payroll/settings', label: 'Cấu hình & điều chỉnh lương', icon: SlidersHorizontal, grant: true, roles: ['HR'] },
  { path: '/payslip', label: 'Phiếu lương cá nhân', icon: Receipt, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  { path: '/income', label: 'Thu nhập khác & doanh thu', icon: Wallet, roles: ['HR', 'MANAGER', 'EMPLOYEE'] },
  { path: '/reports', label: 'Báo cáo', icon: BarChart3, roles: ['ADMIN', 'HR', 'MANAGER'] },
  { path: '/accounts', label: 'Tài khoản & phân quyền', icon: ShieldCheck, roles: ['ADMIN', 'HR'] },
  { path: '/approval-routes', label: 'Tuyến duyệt', icon: GitBranch, roles: ['ADMIN', 'HR'] },
  { path: '/audit', label: 'Nhật ký hệ thống', icon: ScrollText, roles: ['ADMIN', 'HR'] },
  { path: '/catalog', label: 'Danh mục', icon: BookOpen },
  { path: '/notifications', label: 'Thông báo', icon: Bell },
];

export function Sidebar({ isOpen, onClose }) {
  const { role, hasAnyGrant } = useAuth();
  const { pathname } = useLocation();

  const filteredNavItems = NAV_ITEMS.filter((item) => {
    if (item.grant) return hasAnyGrant || Boolean(item.roles?.includes(role));
    return !item.roles || item.roles.includes(role);
  });

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm lg:hidden"
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 flex w-64 flex-col
          border-r border-slate-200 bg-white
          transition-transform duration-200 ease-in-out
          lg:static lg:translate-x-0
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        {/* Brand */}
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200/80 px-6">
          <div className="flex items-center space-x-3">
            <img
              src={logoSvg}
              alt="HRFlow Logo"
              className="h-8 w-8"
            />

            <div>
              <span className="text-lg font-extrabold tracking-tight text-blue-600">
                HR<span className="text-slate-900">Flow</span>
              </span>

              <span className="block text-[10px] font-semibold uppercase tracking-wider text-slate-400">
                Enterprise
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng menu"
            className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 space-y-1 overflow-y-auto px-4 py-6">
          <div className="px-3 pb-2 text-[11px] font-bold uppercase tracking-wider text-slate-400">
            Menu chính
          </div>

          {filteredNavItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-all ${
                    isActive && !(item.path === '/payroll' && pathname.startsWith('/payroll/settings'))
                      ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                <Icon className="mr-3 h-4 w-4 shrink-0" />

                <span className="whitespace-nowrap">
                  {item.label}
                </span>
              </NavLink>
            );
          })}
        </nav>

        {/* Sidebar footer */}
        <div className="shrink-0 border-t border-slate-200/80 bg-slate-50/50 p-4">
          <div className="rounded-xl border border-blue-100 bg-gradient-to-br from-blue-50 to-indigo-50 p-3">
            <p className="text-xs font-bold text-blue-900">
              Quy định pháp lý 2026
            </p>

            <p className="mt-0.5 text-[11px] text-blue-700">
              Áp dụng mức giảm trừ gia cảnh và bậc thuế theo quy định hiện hành.
            </p>
          </div>
        </div>
      </aside>
    </>
  );
}

export default Sidebar;