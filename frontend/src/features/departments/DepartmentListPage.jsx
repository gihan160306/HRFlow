import React, { useState, useEffect } from 'react';
import { Building2, Plus, Users, Edit, Trash2 } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import departmentApi from '../../api/departmentApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import DepartmentForm from './DepartmentForm';

const INITIAL_DEPARTMENTS = [
  {
    id: 1,
    name: 'Ban Giám Đốc',
    code: 'BOD',
    managerName: 'Đặng Minh Hiếu',
    employeeCount: 2,
    description: 'Điều hành chiến lược phát triển toàn diện của doanh nghiệp',
  },
  {
    id: 2,
    name: 'Phòng Nhân Sự',
    code: 'HRD',
    managerName: 'Hồ Thị Gia Hân',
    employeeCount: 4,
    description: 'Quản trị nhân lực, phúc lợi, tuyển dụng và tính lương tự động',
  },
  {
    id: 3,
    name: 'Khối Kỹ Thuật & Công Nghệ',
    code: 'TECH',
    managerName: 'Trần Thị Lệ Hoa',
    employeeCount: 12,
    description: 'Nghiên cứu & phát triển phần mềm, hạ tầng hệ thống và bảo mật',
  },
  {
    id: 4,
    name: 'Phòng Kinh Doanh',
    code: 'SALES',
    managerName: 'Chưa chỉ định',
    employeeCount: 6,
    description: 'Phát triển thị trường, tìm kiếm khách hàng và chăm sóc đối tác',
  },
  {
    id: 5,
    name: 'Phòng Tài Chính - Kế Toán',
    code: 'FIN',
    managerName: 'Chưa chỉ định',
    employeeCount: 3,
    description: 'Quản lý thu chi ngân sách, kiểm toán và báo cáo thuế doanh nghiệp',
  },
];

export function DepartmentListPage() {
  const { isHR, isAdmin } = useAuth();
  const canManage = isHR || isAdmin;

  const [departments, setDepartments] = useState(INITIAL_DEPARTMENTS);
  const [isLoading, setIsLoading] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingDept, setEditingDept] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  useEffect(() => {
    async function fetchDepartments() {
      try {
        setIsLoading(true);
        const data = await departmentApi.getDepartments();
        if (Array.isArray(data) && data.length > 0) {
          setDepartments(data);
        }
      } catch (err) {
        // Fallback to initial
      } finally {
        setIsLoading(false);
      }
    }
    fetchDepartments();
  }, []);

  const handleCreateOrUpdate = (data) => {
    if (editingDept) {
      setDepartments((prev) =>
        prev.map((d) => (d.id === editingDept.id ? { ...d, ...data } : d))
      );
    } else {
      const newDept = {
        id: Date.now(),
        ...data,
        employeeCount: 0,
        managerName: 'Chưa chỉ định',
      };
      setDepartments((prev) => [...prev, newDept]);
    }
    setIsFormOpen(false);
    setEditingDept(null);
  };

  const handleDelete = () => {
    if (deleteTarget) {
      setDepartments((prev) => prev.filter((d) => d.id !== deleteTarget.id));
      setDeleteTarget(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Quản lý Phòng Ban
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Cơ cấu tổ chức và sơ đồ phân bổ nhân sự
          </p>
        </div>

        {canManage && (
          <Button
            onClick={() => {
              setEditingDept(null);
              setIsFormOpen(true);
            }}
            icon={Plus}
          >
            Thêm phòng ban
          </Button>
        )}
      </div>

      {isLoading ? (
        <Loading message="Đang tải danh sách phòng ban..." />
      ) : departments.length === 0 ? (
        <EmptyState
          icon={Building2}
          title="Chưa có phòng ban"
          description="Tạo phòng ban đầu tiên để bắt đầu phân bổ nhân sự."
          actionLabel="Tạo phòng ban"
          onAction={() => setIsFormOpen(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {departments.map((dept) => (
            <div
              key={dept.id}
              className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
                    <Building2 className="w-6 h-6" />
                  </div>
                  <span className="px-2.5 py-1 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg tracking-wider">
                    {dept.code}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-slate-900 mt-4">
                  {dept.name}
                </h3>
                <p className="text-xs text-slate-500 mt-1.5 line-clamp-2">
                  {dept.description || 'Chưa có thông tin mô tả chi tiết.'}
                </p>

                <div className="mt-4 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-600">
                  <div className="flex items-center space-x-1.5">
                    <Users className="w-4 h-4 text-slate-400" />
                    <span className="font-semibold text-slate-900">
                      {dept.employeeCount || 0}
                    </span>{' '}
                    thành viên
                  </div>
                  <span className="text-slate-400">
                    Trưởng bộ phận: <strong className="text-slate-700">{dept.managerName}</strong>
                  </span>
                </div>
              </div>

              {canManage && (
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-end space-x-2">
                  <button
                    onClick={() => {
                      setEditingDept(dept);
                      setIsFormOpen(true);
                    }}
                    className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors"
                    title="Chỉnh sửa"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setDeleteTarget(dept)}
                    className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Xóa"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <DepartmentForm
        isOpen={isFormOpen}
        initialData={editingDept}
        onClose={() => {
          setIsFormOpen(false);
          setEditingDept(null);
        }}
        onSubmit={handleCreateOrUpdate}
      />

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        title="Xóa phòng ban"
        message={`Bạn có chắc muốn xóa "${deleteTarget?.name}"?`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}

export default DepartmentListPage;
