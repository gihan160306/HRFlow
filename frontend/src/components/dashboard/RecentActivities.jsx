import React from 'react';
import { Bell } from 'lucide-react';
import { formatDateTime } from '../../utils/formatDate';

// Backend lưu link thông báo theo đường dẫn API; ánh xạ sang route của frontend.
const LINK_MAP = {
  '/leave-requests/inbox': '/leave/approval',
  '/leave-requests/me': '/leave',
  '/ot-registrations/inbox': '/overtime/approval',
  '/ot-registrations/me': '/overtime/registration',
  '/ot-schedules': '/overtime',
  '/payslips/me': '/payslip',
};

export function toRoute(link) {
  return link ? LINK_MAP[link] || null : null;
}

export function RecentActivities({ notifications = [], onOpen, className = '' }) {
  return (
    <div className={`bg-white rounded-2xl border border-slate-200/80 shadow-sm ${className}`}>
      <div className="p-4 sm:p-6 border-b border-slate-100">
        <h3 className="text-base font-bold text-slate-800">Thông báo gần đây</h3>
      </div>
      {notifications.length === 0 ? (
        <p className="p-6 text-sm text-slate-500">Chưa có thông báo nào.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {notifications.map((n) => {
            const route = toRoute(n.link);
            return (
              <li key={n.id}>
                <button
                  type="button"
                  disabled={!route}
                  onClick={() => route && onOpen?.(route)}
                  className="w-full flex items-start gap-3 p-4 text-left hover:bg-slate-50 disabled:hover:bg-transparent disabled:cursor-default"
                >
                  <span className={`p-2 rounded-lg ${n.readAt ? 'bg-slate-100 text-slate-400' : 'bg-blue-50 text-blue-600'}`}>
                    <Bell className="w-4 h-4" />
                  </span>
                  <span className="flex-1 min-w-0">
                    <span className={`block text-sm ${n.readAt ? 'text-slate-600' : 'font-semibold text-slate-900'}`}>{n.title}</span>
                    <span className="block text-xs text-slate-500 mt-0.5">{n.message}</span>
                    <span className="block text-[11px] text-slate-400 mt-1">{formatDateTime(n.createdAt)}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

export default RecentActivities;
