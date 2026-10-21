import React from 'react';
import { Eye, Edit, Trash2 } from 'lucide-react';
import StatusBadge from '../../components/common/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';

export function EmployeeTable({
  employees = [],
  onView,
  onEdit,
  onDelete,
  canManage = false,
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm text-slate-600">
        <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
          <tr>
            <th className="px-4 py-3.5">Mã NV & Họ tên</th>
            <th className="px-4 py-3.5">Phòng ban</th>
            <th className="px-4 py-3.5">Vị trí</th>
            <th className="px-4 py-3.5">Lương cơ bản</th>
            <th className="px-4 py-3.5">Trạng thái</th>
            <th className="px-4 py-3.5 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {employees.map((emp) => (
            <tr key={emp.id} className="hover:bg-slate-50/80 transition-colors">
              <td className="px-4 py-3.5">
                <div className="flex items-center space-x-3">
                  <img
                    src={
                      emp.avatar ||
                      `https://ui-avatars.com/api/?name=${encodeURIComponent(
                        emp.fullName || 'User'
                      )}&background=3b82f6&color=fff`
                    }
                    alt={emp.fullName}
                    className="w-9 h-9 rounded-full object-cover ring-1 ring-slate-200"
                  />
                  <div>
                    <p className="font-semibold text-slate-900">{emp.fullName}</p>
                    <p className="text-xs text-slate-400">
                      {emp.employeeCode || `EMP-${emp.id}`} • {emp.email}
                    </p>
                  </div>
                </div>
              </td>
              <td className="px-4 py-3.5 font-medium text-slate-700">
                {emp.departmentName || 'Chưa phân bổ'}
              </td>
              <td className="px-4 py-3.5 text-slate-600">{emp.position || '-'}</td>
              <td className="px-4 py-3.5 font-medium text-slate-900">
                {formatCurrency(emp.baseSalary)}
              </td>
              <td className="px-4 py-3.5">
                <StatusBadge status={emp.status || 'ACTIVE'} />
              </td>
              <td className="px-4 py-3.5 text-right">
                <div className="flex items-center justify-end space-x-2">
                  <button
                    onClick={() => onView && onView(emp)}
                    className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    title="Xem chi tiết"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  {canManage && (
                    <>
                      <button
                        onClick={() => onEdit && onEdit(emp)}
                        className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors"
                        title="Chỉnh sửa"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => onDelete && onDelete(emp)}
                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Xóa"
                      >
                        <Trash2 className="w-4 h-4" />
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
