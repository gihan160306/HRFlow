import React from 'react';
import { Eye, Pencil, UserX } from 'lucide-react';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

export function EmployeeTable({ employees = [], onView, onEdit, onTerminate, canManage = false, showContact = true }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm text-slate-600">
        <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
          <tr>
            <th className="px-4 py-3.5">Mã NV & Họ tên</th>
            <th className="px-4 py-3.5">Phòng ban</th>
            <th className="px-4 py-3.5">Chức danh</th>
            {showContact && <th className="px-4 py-3.5">Quản lý trực tiếp</th>}
            {showContact && <th className="px-4 py-3.5">Ngày vào làm</th>}
            <th className="px-4 py-3.5">Trạng thái</th>
            <th className="px-4 py-3.5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {employees.map((emp) => (
            <tr key={emp.id} className="hover:bg-slate-50/80 transition-colors">
              <td className="px-4 py-3.5">
                <p className="font-semibold text-slate-900">{emp.fullName}</p>
                <p className="text-xs text-slate-400">{emp.code} • {emp.workEmail}</p>
              </td>
              <td className="px-4 py-3.5 font-medium text-slate-700">{emp.departmentName}</td>
              <td className="px-4 py-3.5">{emp.jobTitleName}</td>
              {showContact && <td className="px-4 py-3.5">{emp.managerName || '-'}</td>}
              {showContact && <td className="px-4 py-3.5">{formatDate(emp.hireDate)}</td>}
              <td className="px-4 py-3.5"><StatusBadge status={emp.status} /></td>
              <td className="px-4 py-3.5 text-right">
                <div className="flex items-center justify-end space-x-1">
                  <button onClick={() => onView?.(emp)} title="Xem chi tiết" aria-label="Xem chi tiết"
                    className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg">
                    <Eye className="w-4 h-4" />
                  </button>
                  {canManage && emp.status !== 'TERMINATED' && (
                    <>
                      <button onClick={() => onEdit?.(emp)} title="Chỉnh sửa" aria-label="Chỉnh sửa"
                        className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg">
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button onClick={() => onTerminate?.(emp)} title="Cho nghỉ việc" aria-label="Cho nghỉ việc"
                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg">
                        <UserX className="w-4 h-4" />
                      </button>
                    </>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default EmployeeTable;
