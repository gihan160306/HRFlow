import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserPlus, Search } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import employeeApi from '../../api/employeeApi';
import departmentApi from '../../api/departmentApi';
import EmployeeTable from './EmployeeTable';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import Pagination from '../../components/common/Pagination';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import PromptDialog from '../../components/common/PromptDialog';
import { todayIso } from '../../utils/formatDate';

const PAGE_SIZE = 20;
const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Đang làm việc' },
  { value: 'ONBOARDING', label: 'Đang tiếp nhận' },
  { value: 'ON_LEAVE', label: 'Đang nghỉ' },
  { value: 'TERMINATED', label: 'Đã nghỉ việc' },
];

export function EmployeeListPage() {
  const navigate = useNavigate();
  const { user, isHR, isAdmin, isManager } = useAuth();

  const [q, setQ] = useState('');
  const [query, setQuery] = useState('');
  // Manager: bộ lọc mặc định là phòng ban của chính manager đó.
  const [departmentId, setDepartmentId] = useState(
    isManager && user?.departmentId ? String(user.departmentId) : ''
  );
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0); // backend phân trang từ 0
  const [terminateTarget, setTerminateTarget] = useState(null);
  const [terminateDate, setTerminateDate] = useState(todayIso());

  // Debounce ô tìm kiếm để không gọi API mỗi phím gõ.
  useEffect(() => {
    const t = setTimeout(() => {
      setQuery(q.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(t);
  }, [q]);

  const { data: depts } = useAsync(() => departmentApi.list(), []);
  // Manager chỉ thấy team (backend giới hạn) -> chỉ đưa vào bộ lọc các phòng ban có người trong team.
  const { data: team } = useAsync(
    () => employeeApi.list({ page: 0, size: 200 }),
    [],
    { enabled: isManager }
  );

  let deptOptions = (depts || []).map((d) => ({ value: String(d.id), label: d.name }));
  if (isManager) {
    const allowed = new Set((team?.content || []).map((e) => String(e.departmentId)));
    if (user?.departmentId) allowed.add(String(user.departmentId));
    deptOptions = deptOptions.filter((o) => allowed.has(o.value));
  }
  const deptLocked = isManager && deptOptions.length <= 1;
  const { data, loading, error, reload } = useAsync(
    () => employeeApi.list({ page, size: PAGE_SIZE, q: query, departmentId, status }),
    [page, query, departmentId, status]
  );

  const rows = data?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Quản lý Nhân sự</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {isHR && 'Toàn bộ hồ sơ nhân viên của công ty'}
            {isManager && 'Nhân viên thuộc quyền quản lý trực tiếp của bạn'}
            {isAdmin && 'Thông tin định danh tối thiểu phục vụ quản lý tài khoản'}
          </p>
        </div>
        {isHR && (
          <Button onClick={() => navigate('/employees/create')} icon={UserPlus}>
            Thêm nhân viên
          </Button>
        )}
      </div>

      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm grid grid-cols-1 md:grid-cols-3 gap-4">
        <InputField placeholder="Tìm theo họ tên, mã NV, email..." icon={Search} value={q} onChange={(e) => setQ(e.target.value)} />
        <SelectField
          placeholder={deptLocked ? null : 'Tất cả phòng ban'}
          value={departmentId}
          disabled={deptLocked}
          title={deptLocked ? 'Phòng ban bạn quản lý' : undefined}
          onChange={(e) => { setDepartmentId(e.target.value); setPage(0); }}
          options={deptOptions}
        />
        <SelectField
          placeholder="Tất cả trạng thái"
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
          options={STATUS_OPTIONS}
        />
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading message="Đang tải danh sách nhân viên..." />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : rows.length === 0 ? (
          <EmptyState title="Không tìm thấy nhân viên" description="Hãy thử thay đổi từ khóa tìm kiếm hoặc bộ lọc." />
        ) : (
          <>
            <EmployeeTable
              employees={rows}
              canManage={isHR}
              showContact={!isAdmin}
              onView={(emp) => navigate(`/employees/${emp.id}`)}
              onEdit={(emp) => navigate(`/employees/${emp.id}/edit`)}
              onTerminate={(emp) => { setTerminateDate(todayIso()); setTerminateTarget(emp); }}
            />
            <Pagination
              currentPage={page + 1}
              totalPages={data.totalPages}
              totalItems={data.totalElements}
              pageSize={PAGE_SIZE}
              onPageChange={(p) => setPage(p - 1)}
            />
          </>
        )}
      </div>

      <PromptDialog
        isOpen={Boolean(terminateTarget)}
        title="Cho nhân viên nghỉ việc"
        description={terminateTarget && (
          <>
            Nhân viên <b>{terminateTarget.fullName}</b> ({terminateTarget.code}). Hợp đồng đang hiệu lực sẽ bị chấm dứt
            và tài khoản bị vô hiệu hóa sau ngày nghỉ. Dữ liệu lịch sử được giữ nguyên.
            <span className="block mt-3">
              <InputField label="Ngày nghỉ việc" type="date" value={terminateDate} onChange={(e) => setTerminateDate(e.target.value)} required />
            </span>
          </>
        )}
        label="Lý do nghỉ việc"
        confirmLabel="Xác nhận nghỉ việc"
        confirmVariant="danger"
        onClose={() => setTerminateTarget(null)}
        onSubmit={async (reason) => {
          await employeeApi.terminate(terminateTarget.id, { terminationDate: terminateDate, reason });
          reload();
        }}
      />
    </div>
  );
}

export default EmployeeListPage;
