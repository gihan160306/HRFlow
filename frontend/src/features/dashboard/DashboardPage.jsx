import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Clock, CalendarDays, Calculator, FileCheck2, UserPlus, Zap, Bell, Receipt } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import dashboardApi from '../../api/dashboardApi';
import StatCard from '../../components/dashboard/StatCard';
import RecentActivities from '../../components/dashboard/RecentActivities';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import { ROLE_LABELS } from '../../utils/constants';

const has = (v) => v !== undefined && v !== null;

export function DashboardPage() {
  const navigate = useNavigate();
  const { user, role } = useAuth();
  const { data: s, loading, error, reload } = useAsync(() => dashboardApi.summary(), []);
  const { data: notes } = useAsync(() => dashboardApi.notifications({ page: 0, size: 6 }), []);

  // Chỉ hiện thẻ mà backend trả về cho role/grant hiện tại (không suy đoán, không số giả).
  const cards = s
    ? [
        has(s.activeEmployees) && { title: 'Nhân sự đang làm việc', value: s.activeEmployees, icon: Users, color: 'blue', to: '/employees' },
        has(s.onboarding) && { title: 'Đang tiếp nhận', value: s.onboarding, icon: UserPlus, color: 'purple', to: '/employees' },
        has(s.expiringContracts30d) && { title: 'Hợp đồng hết hạn ≤ 30 ngày', value: s.expiringContracts30d, icon: FileCheck2, color: 'rose', to: '/contracts' },
        has(s.attendanceNeedsReviewThisMonth) && { title: 'Chấm công cần rà soát (tháng này)', value: s.attendanceNeedsReviewThisMonth, icon: Clock, color: 'amber', to: '/attendance' },
        has(s.pendingAccountRequests) && { title: 'Yêu cầu tài khoản chờ xử lý', value: s.pendingAccountRequests, icon: UserPlus, color: 'amber' },
        has(s.teamSize) && s.teamSize > 0 && { title: 'Quy mô team', value: s.teamSize, icon: Users, color: 'emerald', to: '/employees' },
        has(s.pendingLeaveForMe) && s.pendingLeaveForMe > 0 && { title: 'Đơn nghỉ phép chờ tôi duyệt', value: s.pendingLeaveForMe, icon: CalendarDays, color: 'amber', to: '/leave/approval' },
        has(s.pendingOtForMe) && s.pendingOtForMe > 0 && { title: 'Đăng ký OT chờ tôi duyệt', value: s.pendingOtForMe, icon: Zap, color: 'amber', to: '/overtime/approval' },
        has(s.myPendingLeave) && { title: 'Đơn nghỉ của tôi đang chờ', value: s.myPendingLeave, icon: CalendarDays, color: 'blue', to: '/leave' },
        has(s.openPayrollPeriods) && { title: 'Kỳ lương chưa khóa', value: s.openPayrollPeriods, icon: Calculator, color: 'purple', to: '/payroll' },
        has(s.unreadNotifications) && { title: 'Thông báo chưa đọc', value: s.unreadNotifications, icon: Bell, color: 'emerald' },
      ].filter(Boolean)
    : [];

  const canSelfService = Boolean(user?.employeeId);

  return (
    <div className="space-y-6">
      <div className="rounded-3xl bg-gradient-to-r from-blue-600 via-indigo-600 to-blue-800 p-6 sm:p-8 text-white shadow-xl shadow-blue-500/10">
        <p className="text-xs font-semibold uppercase tracking-wider text-blue-100">{ROLE_LABELS[role] || role}</p>
        <h1 className="mt-1 text-2xl sm:text-3xl font-extrabold tracking-tight">
          Xin chào, {user?.fullName || 'Đồng nghiệp'}!
        </h1>
        <p className="mt-2 text-sm sm:text-base text-blue-100">
          {new Date().toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
        {canSelfService && (
          <div className="mt-6 flex flex-wrap gap-3">
            <Button variant="secondary" size="sm" onClick={() => navigate('/attendance')} className="bg-white text-blue-700 hover:bg-blue-50">
              Chấm công
            </Button>
            <Button variant="outline" size="sm" onClick={() => navigate('/leave')} className="border-white/30 text-white hover:bg-white/10">
              Tạo đơn nghỉ phép
            </Button>
            <Button variant="outline" size="sm" onClick={() => navigate('/payslip')} icon={Receipt} className="border-white/30 text-white hover:bg-white/10">
              Phiếu lương
            </Button>
          </div>
        )}
      </div>

      {loading && <Loading message="Đang tải tổng quan..." />}
      {error && <ErrorMessage message={error.message} onRetry={reload} />}

      {cards.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {cards.map((c) => (
            <button
              key={c.title}
              type="button"
              disabled={!c.to}
              onClick={() => c.to && navigate(c.to)}
              className="text-left disabled:cursor-default"
            >
              <StatCard title={c.title} value={c.value} icon={c.icon} color={c.color} />
            </button>
          ))}
        </div>
      )}

      <RecentActivities
        notifications={notes?.content || []}
        onOpen={(route) => navigate(route)}
      />
    </div>
  );
}

export default DashboardPage;
