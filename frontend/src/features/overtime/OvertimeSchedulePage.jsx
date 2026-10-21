import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Zap, Plus, Calendar, Clock, UserCheck } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

const SAMPLE_SCHEDULES = [
  {
    id: 1,
    title: 'Tăng ca bảo trì hệ thống cuối tuần',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    date: '2026-10-24',
    startTime: '09:00',
    endTime: '17:00',
    rateMultiplier: '2.0x (Ngày nghỉ cuối tuần)',
    plannedStaff: 4,
    status: 'ACTIVE',
  },
  {
    id: 2,
    title: 'Deploy phiên bản tính lương v2.0',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    date: '2026-10-21',
    startTime: '18:00',
    endTime: '22:00',
    rateMultiplier: '1.5x (Ngày thường)',
    plannedStaff: 6,
    status: 'ACTIVE',
  },
];

export function OvertimeSchedulePage() {
  const navigate = useNavigate();
  const { isManager, isHR, isAdmin } = useAuth();
  const canManage = isManager || isHR || isAdmin;

  const [schedules] = useState(SAMPLE_SCHEDULES);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Kế hoạch Tăng ca (OT Schedules)
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Lịch làm việc thêm giờ theo quy định và hệ số thù lao (150%, 200%, 300%)
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Button
            variant="outline"
            onClick={() => navigate('/overtime/registration')}
            icon={Zap}
          >
            Đăng ký OT cá nhân
          </Button>
          {canManage && (
            <Button
              variant="outline"
              onClick={() => navigate('/overtime/approval')}
              icon={UserCheck}
            >
              Duyệt đăng ký OT
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {schedules.map((sch) => (
          <div
            key={sch.id}
            className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm flex flex-col justify-between"
          >
            <div>
              <div className="flex items-start justify-between">
                <span className="p-3 bg-amber-50 text-amber-600 rounded-xl">
                  <Zap className="w-6 h-6" />
                </span>
                <StatusBadge status={sch.status} />
              </div>

              <h3 className="text-lg font-bold text-slate-900 mt-4">
                {sch.title}
              </h3>
              <p className="text-xs text-slate-500 mt-1">{sch.department}</p>

              <div className="mt-4 bg-slate-50 rounded-xl p-3.5 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-500 flex items-center">
                    <Calendar className="w-3.5 h-3.5 mr-1.5 text-slate-400" />
                    Ngày làm việc:
                  </span>
                  <span className="font-semibold text-slate-800">
                    {formatDate(sch.date)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500 flex items-center">
                    <Clock className="w-3.5 h-3.5 mr-1.5 text-slate-400" />
                    Thời gian:
                  </span>
                  <span className="font-semibold text-slate-800">
                    {sch.startTime} - {sch.endTime}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Hệ số lương OT:</span>
                  <span className="font-bold text-amber-600">
                    {sch.rateMultiplier}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
              <span className="text-xs text-slate-400">
                Chỉ tiêu: <strong className="text-slate-700">{sch.plannedStaff} người</strong>
              </span>
              <Button
                size="sm"
                onClick={() => navigate('/overtime/registration')}
              >
                Đăng ký ca này
              </Button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default OvertimeSchedulePage;
