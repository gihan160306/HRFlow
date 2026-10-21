import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import {
  Users,
  Clock,
  CalendarDays,
  Calculator,
  ArrowRight,
  Sparkles,
} from 'lucide-react';
import StatCard from '../../components/dashboard/StatCard';
import RecentActivities from '../../components/dashboard/RecentActivities';
import Button from '../../components/common/Button';

export function DashboardPage() {
  const navigate = useNavigate();
  const { user, role, isEmployee } = useAuth();

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-blue-600 via-indigo-600 to-blue-800 p-6 sm:p-8 text-white shadow-xl shadow-blue-500/10">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center space-x-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold backdrop-blur-md mb-3 border border-white/10">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Hệ thống Nhân sự & Tiền lương 2026</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Xin chào, {user?.fullName || 'Đồng nghiệp'}!
          </h1>
          <p className="mt-2 text-sm sm:text-base text-blue-100">
            Hôm nay là {new Date().toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}.
            Chúc bạn một ngày làm việc tràn đầy năng lượng và hiệu quả.
          </p>

          <div className="mt-6 flex flex-wrap gap-3">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => navigate('/attendance')}
              className="bg-white text-blue-700 hover:bg-blue-50"
            >
              Chấm công ngay
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/leave')}
              className="border-white/30 text-white hover:bg-white/10"
            >
              Tạo đơn nghỉ phép
            </Button>
          </div>
        </div>

        {/* Decorative circle */}
        <div className="absolute -right-10 -bottom-10 w-64 h-64 rounded-full bg-white/5 pointer-events-none" />
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <StatCard
          title="Tổng nhân sự"
          value="48"
          change="+3 nhân viên"
          isPositive={true}
          icon={Users}
          color="blue"
          subtitle="so với tháng trước"
        />
        <StatCard
          title="Tỷ lệ có mặt hôm nay"
          value="95.8%"
          change="46/48 nhân viên"
          isPositive={true}
          icon={Clock}
          color="emerald"
        />
        <StatCard
          title="Đơn nghỉ phép chờ duyệt"
          value="5"
          change="3 đơn mới"
          isPositive={false}
          icon={CalendarDays}
          color="amber"
          subtitle="cần xử lý"
        />
        <StatCard
          title="Quỹ lương tháng 10"
          value="1.25 Tỷ ₫"
          change="+4.2%"
          isPositive={true}
          icon={Calculator}
          color="purple"
          subtitle="đã khóa sổ"
        />
      </div>

      {/* Main Grid: Quick Actions & Recent Activities */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Quick Access Tiles */}
        <div className="lg:col-span-1 space-y-4">
          <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm">
            <h3 className="text-base font-bold text-slate-800 mb-4">
              Phím tắt chức năng
            </h3>
            <div className="space-y-2.5">
              <button
                onClick={() => navigate('/attendance')}
                className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-blue-200 hover:bg-blue-50/50 transition-colors text-left"
              >
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                    <Clock className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-slate-800">Điểm danh Check-in / Out</p>
                    <p className="text-[11px] text-slate-500">Ghi nhận giờ làm việc hôm nay</p>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => navigate('/leave')}
                className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-amber-200 hover:bg-amber-50/50 transition-colors text-left"
              >
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
                    <CalendarDays className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-slate-800">Xin nghỉ phép (PTO)</p>
                    <p className="text-[11px] text-slate-500">Xem phép năm & gửi yêu cầu</p>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => navigate('/payslip')}
                className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-emerald-200 hover:bg-emerald-50/50 transition-colors text-left"
              >
                <div className="flex items-center space-x-3">
                  <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
                    <Calculator className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-slate-800">Xem phiếu lương cá nhân</p>
                    <p className="text-[11px] text-slate-500">Chi tiết lương Gross - Net & bảo hiểm</p>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400" />
              </button>
            </div>
          </div>
        </div>

        {/* Recent Activities */}
        <div className="lg:col-span-2">
          <RecentActivities />
        </div>
      </div>
    </div>
  );
}

export default DashboardPage;
