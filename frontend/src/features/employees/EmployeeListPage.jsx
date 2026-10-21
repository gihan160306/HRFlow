import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserPlus, Search, Filter } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import employeeApi from '../../api/employeeApi';
import EmployeeTable from './EmployeeTable';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import Pagination from '../../components/common/Pagination';
import Loading from '../../components/common/Loading';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';

const INITIAL_EMPLOYEES = [
  {
    id: 1,
    employeeCode: 'EMP-001',
    fullName: 'Đặng Minh Hiếu',
    email: 'admin@example.com',
    phone: '0901234567',
    departmentName: 'Ban Giám Đốc',
    position: 'Giám Đốc Vận Hành / Admin',
    baseSalary: 45000000,
    status: 'ACTIVE',
  },
  {
    id: 2,
    employeeCode: 'EMP-002',
    fullName: 'Hồ Thị Gia Hân',
    email: 'hr@example.com',
    phone: '0902345678',
    departmentName: 'Phòng Nhân Sự',
    position: 'Trưởng Phòng Nhân Sự / HR',
    baseSalary: 32000000,
    status: 'ACTIVE',
  },
  {
    id: 3,
    employeeCode: 'EMP-003',
    fullName: 'Trần Thị Lệ Hoa',
    email: 'manager@example.com',
    phone: '0903456789',
    departmentName: 'Khối Kỹ Thuật & Công Nghệ',
    position: 'Giám Đốc Kỹ Thuật / Manager',
    baseSalary: 38000000,
    status: 'ACTIVE',
  },
  {
    id: 4,
    employeeCode: 'EMP-004',
    fullName: 'Nguyễn Hoàng Phương Ngân',
    email: 'employee1@example.com',
    phone: '0904567890',
    departmentName: 'Khối Kỹ Thuật & Công Nghệ',
    position: 'Senior Software Engineer',
    baseSalary: 28000000,
    status: 'ACTIVE',
  },
  {
    id: 5,
    employeeCode: 'EMP-005',
    fullName: 'Hồ Vinh Quan',
    email: 'hqvinh@example.com',
    phone: '0905678901',
    departmentName: 'Khối Kỹ Thuật & Công Nghệ',
    position: 'Fullstack Developer',
    baseSalary: 22000000,
    status: 'ACTIVE',
  },
];

export function EmployeeListPage() {
  const navigate = useNavigate();
  const { isHR, isAdmin } = useAuth();
  const canManage = isHR || isAdmin;

  const [employees, setEmployees] = useState(INITIAL_EMPLOYEES);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDept, setSelectedDept] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const [page, setPage] = useState(1);
  const pageSize = 5;

  useEffect(() => {
    async function loadData() {
      try {
        setIsLoading(true);
        const data = await employeeApi.getEmployees();
        if (Array.isArray(data) && data.length > 0) {
          setEmployees(data);
        }
      } catch (err) {
        // Fallback to initial employees
      } finally {
        setIsLoading(false);
      }
    }
    loadData();
  }, []);

  const filtered = employees.filter((emp) => {
    const matchSearch =
      (emp.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (emp.employeeCode || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (emp.email || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchDept = selectedDept ? emp.departmentName === selectedDept : true;
    return matchSearch && matchDept;
  });

  const totalPages = Math.ceil(filtered.length / pageSize) || 1;
  const pagedEmployees = filtered.slice((page - 1) * pageSize, page * pageSize);

  const handleDeleteConfirm = () => {
    if (deleteTarget) {
      setEmployees((prev) => prev.filter((e) => e.id !== deleteTarget.id));
      setDeleteTarget(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Quản lý Nhân sự
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Danh sách hồ sơ nhân viên và thông tin hợp đồng
          </p>
        </div>

        {canManage && (
          <Button
            onClick={() => navigate('/employees/create')}
            icon={UserPlus}
            className="self-start sm:self-auto"
          >
            Thêm nhân viên
          </Button>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm flex flex-col md:flex-row items-center gap-4">
        <div className="flex-1 w-full">
          <InputField
            placeholder="Tìm theo họ tên, mã NV, email..."
            icon={Search}
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(1);
            }}
          />
        </div>

        <div className="w-full md:w-64">
          <select
            value={selectedDept}
            onChange={(e) => {
              setSelectedDept(e.target.value);
              setPage(1);
            }}
            className="w-full rounded-lg border border-slate-300 py-2.5 px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
          >
            <option value="">Tất cả phòng ban</option>
            <option value="Ban Giám Đốc">Ban Giám Đốc</option>
            <option value="Phòng Nhân Sự">Phòng Nhân Sự</option>
            <option value="Khối Kỹ Thuật & Công Nghệ">Khối Kỹ Thuật & Công Nghệ</option>
          </select>
        </div>
      </div>

      {/* Table Container */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {isLoading ? (
          <Loading message="Đang tải danh sách nhân viên..." />
        ) : filtered.length === 0 ? (
          <EmptyState
            title="Không tìm thấy nhân viên"
            description="Hãy thử thay đổi từ khóa tìm kiếm hoặc bộ lọc phòng ban."
          />
        ) : (
          <>
            <EmployeeTable
              employees={pagedEmployees}
              canManage={canManage}
              onView={(emp) => navigate(`/employees/${emp.id}`)}
              onEdit={(emp) => navigate(`/employees/${emp.id}?edit=true`)}
              onDelete={(emp) => setDeleteTarget(emp)}
            />
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              totalItems={filtered.length}
              pageSize={pageSize}
              onPageChange={setPage}
            />
          </>
        )}
      </div>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        title="Xóa nhân viên"
        message={`Bạn có chắc chắn muốn xóa hồ sơ nhân viên "${deleteTarget?.fullName}" (${deleteTarget?.employeeCode}) không?`}
        confirmLabel="Xóa hồ sơ"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}

export default EmployeeListPage;
