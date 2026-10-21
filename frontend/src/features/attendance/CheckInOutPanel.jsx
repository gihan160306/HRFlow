import React, { useState, useEffect } from 'react';
import { Clock, LogIn, LogOut, CheckCircle, AlertCircle } from 'lucide-react';
import Button from '../../components/common/Button';
import attendanceApi from '../../api/attendanceApi';
import { formatTime } from '../../utils/formatDate';

export function CheckInOutPanel({ onAttendanceUpdate }) {
  const [currentTime, setCurrentTime] = useState(new Date());
  const [todayRecord, setTodayRecord] = useState({
    checkIn: '08:28',
    checkOut: null,
    status: 'PRESENT',
  });
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const handleCheckIn = async () => {
    setIsLoading(true);
    try {
      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(
        now.getMinutes()
      ).padStart(2, '0')}`;
      await attendanceApi.checkIn().catch(() => null);
      setTodayRecord((prev) => ({
        ...prev,
        checkIn: timeStr,
        status: 'PRESENT',
      }));
      if (onAttendanceUpdate) onAttendanceUpdate();
    } finally {
      setIsLoading(false);
    }
  };

  const handleCheckOut = async () => {
    setIsLoading(true);
    try {
      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(
        now.getMinutes()
      ).padStart(2, '0')}`;
      await attendanceApi.checkOut().catch(() => null);
      setTodayRecord((prev) => ({
        ...prev,
        checkOut: timeStr,
      }));
      if (onAttendanceUpdate) onAttendanceUpdate();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-3xl border border-slate-200/80 shadow-sm p-6 sm:p-8 flex flex-col md:flex-row items-center justify-between gap-6">
      {/* Live Clock Section */}
      <div className="flex items-center space-x-5 text-center md:text-left">
        <div className="p-4 bg-blue-50 text-blue-600 rounded-2xl">
          <Clock className="w-8 h-8" />
        </div>
        <div>
          <p className="text-xs uppercase font-bold text-slate-400 tracking-wider">
            Đồng hồ điểm danh thời gian thực
          </p>
          <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 font-mono tracking-tight">
            {currentTime.toLocaleTimeString('vi-VN')}
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Ca làm việc tiêu chuẩn: 08:30 - 17:30 (Thứ 2 - Thứ 6)
          </p>
        </div>
      </div>

      {/* Check In / Out Buttons & Today Status */}
      <div className="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
        <div className="bg-slate-50 rounded-2xl p-3 border border-slate-200/60 flex items-center space-x-4 text-xs w-full sm:w-auto justify-around">
          <div>
            <span className="text-slate-400 block">Vào ca</span>
            <span className="font-bold text-slate-800 text-sm">
              {todayRecord.checkIn || '--:--'}
            </span>
          </div>
          <div className="h-6 w-px bg-slate-200" />
          <div>
            <span className="text-slate-400 block">Ra ca</span>
            <span className="font-bold text-slate-800 text-sm">
              {todayRecord.checkOut || '--:--'}
            </span>
          </div>
        </div>

        <div className="flex items-center space-x-3 w-full sm:w-auto">
          <Button
            variant="primary"
            onClick={handleCheckIn}
            isLoading={isLoading}
            disabled={Boolean(todayRecord.checkIn)}
            icon={LogIn}
            className="flex-1 sm:flex-initial"
          >
            {todayRecord.checkIn ? 'Đã Check-in' : 'Check-in'}
          </Button>

          <Button
            variant="outline"
            onClick={handleCheckOut}
            isLoading={isLoading}
            disabled={!todayRecord.checkIn || Boolean(todayRecord.checkOut)}
            icon={LogOut}
            className="flex-1 sm:flex-initial"
          >
            {todayRecord.checkOut ? 'Đã Check-out' : 'Check-out'}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default CheckInOutPanel;
