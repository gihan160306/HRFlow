import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import {
  Bell,
  LogOut,
  User,
  ShieldCheck,
  ChevronDown,
  Menu,
} from 'lucide-react';
import { ROLES } from '../utils/constants';

export function Header({ onMenuClick }) {
  const { user, role, logout, switchRole, defaultUsers } = useAuth();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showRoleSwitcher, setShowRoleSwitcher] = useState(false);

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-200/80 bg-white/95 px-4 lg:px-8 backdrop-blur transition-all">
      <div className="flex items-center space-x-3">
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:block">
          <h2 className="text-sm font-semibold text-slate-800">
            Hệ thống Quản lý Nhân sự & Tính lương
          </h2>
          <p className="text-xs text-slate-500">Doanh nghiệp HRFlow</p>
        </div>
      </div>

      <div className="flex items-center space-x-3 sm:space-x-4">
        {/* Quick Role Switcher for Demo testing */}
        <div className="relative">
          <button
            onClick={() => setShowRoleSwitcher(!showRoleSwitcher)}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors"
            title="Chuyển vai trò thử nghiệm"
          >
            <ShieldCheck className="w-3.5 h-3.5 text-blue-600" />
            <span>Vai trò: {role}</span>
            <ChevronDown className="w-3 h-3 text-blue-500" />
          </button>

          {showRoleSwitcher && (
            <div className="absolute right-0 mt-2 w-48 rounded-xl bg-white p-1.5 shadow-lg border border-slate-100 z-50">
              <div className="px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Thử nghiệm vai trò
              </div>
              {Object.values(ROLES).map((r) => (
                <button
                  key={r}
                  onClick={() => {
                    switchRole(r);
                    setShowRoleSwitcher(false);
                  }}
                  className={`flex w-full items-center justify-between px-3 py-2 text-xs rounded-lg transition-colors ${
                    role === r
                      ? 'bg-blue-50 text-blue-700 font-bold'
                      : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <span>{r}</span>
                  {role === r && <span className="h-1.5 w-1.5 rounded-full bg-blue-600" />}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Notifications */}
        <button
          className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-full transition-colors"
          title="Thông báo"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-1 right-1 h-2 w-2 rounded-full bg-red-500"></span>
        </button>

        {/* User profile */}
        <div className="relative">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="flex items-center space-x-2 p-1.5 rounded-xl hover:bg-slate-100 transition-colors"
          >
            <img
              src={
                user?.avatar ||
                'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80'
              }
              alt={user?.fullName || 'User'}
              className="w-8 h-8 rounded-full object-cover ring-2 ring-blue-500/20"
            />
            <div className="hidden md:block text-left">
              <p className="text-xs font-bold text-slate-800 leading-tight">
                {user?.fullName || 'Người dùng'}
              </p>
              <p className="text-[11px] text-slate-500 leading-tight">
                {user?.email || 'user@example.com'}
              </p>
            </div>
            <ChevronDown className="w-4 h-4 text-slate-400 hidden md:block" />
          </button>

          {showUserMenu && (
            <div className="absolute right-0 mt-2 w-56 rounded-xl bg-white p-1.5 shadow-xl border border-slate-100 z-50">
              <div className="px-3 py-2 border-b border-slate-100 mb-1">
                <p className="text-xs font-bold text-slate-900">{user?.fullName}</p>
                <p className="text-[11px] text-slate-500">{user?.email}</p>
                <span className="inline-block mt-1 text-[10px] font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-100">
                  {role}
                </span>
              </div>

              <button
                onClick={() => {
                  setShowUserMenu(false);
                  logout();
                }}
                className="flex w-full items-center px-3 py-2 text-xs text-red-600 hover:bg-red-50 rounded-lg transition-colors font-medium"
              >
                <LogOut className="w-4 h-4 mr-2" />
                Đăng xuất
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;
