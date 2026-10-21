import React from 'react';
import { Clock, UserCheck, CalendarCheck, FileText, CheckCircle2 } from 'lucide-react';
import { formatDateTime } from '../../utils/formatDate';

const DEFAULT_ACTIVITIES = [
  {
    id: 1,
    user: 'Hồ Thị Gia Hân (HR)',
    action: 'Duyệt bảng lương tháng 10/2026',
    time: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
    icon: CheckCircle2,
    color: 'text-emerald-600 bg-emerald-50',
  },
  {
    id: 2,
    user: 'Đặng Minh Hiếu (Admin)',
    action: 'Cập nhật cấu hình biểu thuế TNCN 2026',
    time: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
    icon: FileText,
    color: 'text-blue-600 bg-blue-50',
  },
  {
    id: 3,
    user: 'Nguyễn Hoàng Phương Ngân',
    action: 'Check-in đúng giờ ca sáng (08:28)',
    time: new Date(Date.now() - 1000 * 60 * 240).toISOString(),
    icon: UserCheck,
    color: 'text-indigo-600 bg-indigo-50',
  },
  {
    id: 4,
    user: 'Trần Thị Lệ Hoa (Manager)',
    action: 'Duyệt yêu cầu nghỉ phép cho Nguyễn Hoàng Phương Ngân',
    time: new Date(Date.now() - 1000 * 60 * 360).toISOString(),
    icon: CalendarCheck,
    color: 'text-amber-600 bg-amber-50',
  },
];

export function RecentActivities({ activities = DEFAULT_ACTIVITIES, className = '' }) {
  return (
    <div
      className={`bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm ${className}`}
    >
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-bold text-slate-800 flex items-center">
          <Clock className="w-5 h-5 mr-2 text-blue-600" />
          Hoạt động gần đây
        </h3>
        <span className="text-xs text-slate-400">Thời gian thực</span>
      </div>

      <div className="flow-root">
        <ul className="-mb-8">
          {activities.map((activity, activityIdx) => {
            const Icon = activity.icon || Clock;
            return (
              <li key={activity.id || activityIdx}>
                <div className="relative pb-8">
                  {activityIdx !== activities.length - 1 ? (
                    <span
                      className="absolute top-4 left-4 -ml-px h-full w-0.5 bg-slate-200"
                      aria-hidden="true"
                    />
                  ) : null}
                  <div className="relative flex space-x-3">
                    <div>
                      <span
                        className={`h-8 w-8 rounded-full flex items-center justify-center ring-8 ring-white ${
                          activity.color || 'bg-blue-50 text-blue-600'
                        }`}
                      >
                        <Icon className="h-4 w-4" aria-hidden="true" />
                      </span>
                    </div>
                    <div className="flex-1 min-w-0 pt-1.5 flex justify-between space-x-4">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">
                          {activity.user}
                        </p>
                        <p className="text-sm text-slate-600 mt-0.5">
                          {activity.action}
                        </p>
                      </div>
                      <div className="text-right text-xs whitespace-nowrap text-slate-400">
                        {formatDateTime(activity.time)}
                      </div>
                    </div>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

export default RecentActivities;
