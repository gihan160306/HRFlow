import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, LogOut, ChevronDown, Menu, KeyRound, UserRound } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import dashboardApi from '../api/dashboardApi';
import { ROLE_LABELS } from '../utils/constants';

const POLL_MS = 45000;

export function Header({ onMenuClick }) {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [unread, setUnread] = useState(0);

  // Số thông báo chưa đọc lấy từ backend, cập nhật định kỳ.
  useEffect(() => {
    let cancelled = false;
    const load = () =>
      dashboardApi
        .unreadCount()
        .then((r) => !cancelled && setUnread(Number(r?.count) || 0))
        .catch(() => {});
    load();
    const timer = setInterval(load, POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  const initials = (user?.fullName || '?')
    .split(' ')
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-200/80 bg-white/95 px-4 lg:px-8 backdrop-blur">
      <div className="flex items-center space-x-3">
        <button
          onClick={onMenuClick}
          aria-label="Mở menu"
          className="lg:hidden p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:block">
          <h2 className="text-sm font-semibold text-slate-800">Hệ thống Quản lý Nhân sự & Tính lương</h2>
          <p className="text-xs text-slate-500">HRFlow</p>
        </div>
      </div>

      <div className="flex items-center space-x-3 sm:space-x-4">
        <button
          className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-full transition-colors"
          title={`${unread} thông báo chưa đọc`}
          aria-label="Thông báo"
        >
          <Bell className="w-5 h-5" />
          {unread > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
              {unread > 99 ? '99+' : unread}
            </span>
          )}
        </button>

        <div className="relative">
          <button
            onClick={() => setShowUserMenu((v) => !v)}
            className="flex items-center space-x-2 p-1.5 rounded-xl hover:bg-slate-100 transition-colors"
          >
            <span className="w-8 h-8 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center ring-2 ring-blue-500/20">
              {initials}
            </span>
            <div className="hidden md:block text-left">
              <p className="text-xs font-bold text-slate-800 leading-tight">{user?.fullName}</p>
              <p className="text-[11px] text-slate-500 leading-tight">{user?.email}</p>
            </div>
            <ChevronDown className="w-4 h-4 text-slate-400 hidden md:block" />
          </button>

          {showUserMenu && (
            <div className="absolute right-0 mt-2 w-60 rounded-xl bg-white p-1.5 shadow-xl border border-slate-100 z-50">
              <div className="px-3 py-2 border-b border-slate-100 mb-1">
                <p className="text-xs font-bold text-slate-900">{user?.fullName}</p>
                <p className="text-[11px] text-slate-500">{user?.email}</p>
                {user?.jobTitleName && <p className="text-[11px] text-slate-500">{user.jobTitleName}</p>}
                <span className="inline-block mt-1 text-[10px] font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-100">
                  {ROLE_LABELS[role] || role}
                </span>
              </div>

              {user?.employeeId && (
                <button
                  onClick={() => {
                    setShowUserMenu(false);
                    navigate('/profile');
                  }}
                  className="flex w-full items-center px-3 py-2 text-xs text-slate-700 hover:bg-slate-50 rounded-lg transition-colors font-medium"
                >
                  <UserRound className="w-4 h-4 mr-2" />
                  Hồ sơ của tôi
                </button>
              )}

              <button
                onClick={() => {
                  setShowUserMenu(false);
                  navigate('/change-password');
                }}
                className="flex w-full items-center px-3 py-2 text-xs text-slate-700 hover:bg-slate-50 rounded-lg transition-colors font-medium"
              >
                <KeyRound className="w-4 h-4 mr-2" />
                Đổi mật khẩu
              </button>

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
