import React, { useState } from 'react';
import CheckInOutPanel from './CheckInOutPanel';
import AttendanceHistory from './AttendanceHistory';
import { Calendar, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';
import StatCard from '../../components/dashboard/StatCard';

const SAMPLE_ATTENDANCE = [
  { id: 1, date: '2026-10-17', checkIn: '08:28', checkOut: null, workHours: 8.0, status: 'PRESENT' },
  { id: 2, date: '2026-10-16', checkIn: '08:30', checkOut: '17:35', workHours: 8.0, status: 'PRESENT' },
  { id: 3, date: '2026-10-15', checkIn: '08:42', checkOut: '17:30', workHours: 7.8, status: 'LATE' },
  { id: 4, date: '2026-10-14', checkIn: '08:25', checkOut: '17:30', workHours: 8.0, status: 'PRESENT' },
  { id: 5, date: '2026-10-13', checkIn: '08:29', checkOut: '17:31', workHours: 8.0, status: 'PRESENT' },
];

export function AttendancePage() {
  const [records, setRecords] = useState(SAMPLE_ATTENDANCE);

  const handleUpdate = () => {
    // Add today's live record
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
          Chấm công & Điểm danh
        </h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Ghi nhận thời gian làm việc hàng ngày và theo dõi công chuẩn
        </p>
      </div>

      {/* Main CheckInOut Card */}
      <CheckInOutPanel onAttendanceUpdate={handleUpdate} />

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Số công trong tháng"
          value="14 / 22"
          change="Đạt 63.6%"
          isPositive={true}
          icon={CheckCircle2}
          color="blue"
        />
        <StatCard
          title="Tổng giờ làm việc"
          value="112.5 giờ"
          change="8.0h / ngày"
          isPositive={true}
          icon={Clock}
          color="emerald"
        />
        <StatCard
          title="Số lần đi muộn"
          value="1 lần"
          change="12 phút"
          isPositive={false}
          icon={AlertTriangle}
          color="amber"
        />
        <StatCard
          title="Nghỉ có hưởng lương"
          value="1 ngày"
          change="Phép năm"
          isPositive={true}
          icon={Calendar}
          color="purple"
        />
      </div>

      {/* History table */}
      <AttendanceHistory records={records} />
    </div>
  );
}

export default AttendancePage;
