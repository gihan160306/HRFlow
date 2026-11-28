import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, RotateCcw } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import payrollApi from '../../api/payrollApi';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

// Đúng 4 trạng thái kỳ lương chuẩn (SRS FR-13, BR-05); PAID là trạng thái chi trả riêng (BR-28).
const PERIOD_STATUSES = [
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'CALCULATED', label: 'Đã tính' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'LOCKED', label: 'Đã khóa' },
];

export function PayrollListPage() {
  const navigate = useNavigate();
  const { hasGrant, grants } = useAuth();
  const { data, loading, error, reload } = useAsync(() => payrollApi.periods(), []);

  const now = new Date();
  const [yearFilter, setYearFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ year: now.getFullYear(), month: now.getMonth() + 1, payDate: '' });
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const yearOptions = useMemo(() => {
    const years = Array.from(new Set((data || []).map((p) => p.year).filter(Boolean))).sort((a, b) => b - a);
    return years.map((y) => ({ value: String(y), label: `Năm ${y}` }));
  }, [data]);

  const filtered = useMemo(() => {
    const list = data || [];
    return list.filter((p) => (!yearFilter || String(p.year) === yearFilter) && (!statusFilter || p.status === statusFilter));
  }, [data, yearFilter, statusFilter]);

  const hasFilter = Boolean(yearFilter || statusFilter);
  const clearFilters = () => {
    setYearFilter('');
    setStatusFilter('');
  };

  const create = async (e) => {
    e.preventDefault();
    setBusy(true);
    setFormError('');
    try {
      const p = await payrollApi.createPeriod({
        year: Number(form.year), month: Number(form.month), payDate: form.payDate || undefined,
      });
      setOpen(false);
      navigate(`/payroll/${p.id}`);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Kỳ lương</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Quyền của bạn: {grants.length ? grants.join(', ') : 'không có'} (cấp riêng theo tài khoản, không theo vai trò)
          </p>
        </div>
        {hasGrant('PREPARE') && <Button icon={Plus} onClick={() => { setFormError(''); setOpen(true); }}>Tạo kỳ lương</Button>}
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-4">
        <div className="flex flex-col sm:flex-row sm:items-end gap-3">
          <SelectField
            id="payroll-filter-year"
            label="Năm"
            placeholder="Tất cả năm"
            options={yearOptions}
            value={yearFilter}
            onChange={(e) => setYearFilter(e.target.value)}
            className="sm:w-44"
          />
          <SelectField
            id="payroll-filter-status"
            label="Trạng thái kỳ lương"
            placeholder="Tất cả trạng thái"
            options={PERIOD_STATUSES}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="sm:w-52"
          />
          {hasFilter && (
            <Button variant="outline" icon={RotateCcw} onClick={clearFilters}>
              Xóa bộ lọc
            </Button>
          )}
          {!loading && !error && Boolean(data?.length) && (
            <p className="text-xs text-slate-500 sm:ml-auto sm:pb-2.5">
              Hiển thị <span className="font-semibold text-slate-700">{filtered.length}</span> / {data.length} kỳ lương
            </p>
          )}
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Chưa có kỳ lương" />
        ) : !filtered.length ? (
          <EmptyState
            title="Không có kỳ lương phù hợp"
            description="Không tìm thấy kỳ lương khớp với bộ lọc năm và trạng thái hiện tại."
            actionLabel="Xóa bộ lọc"
            onAction={clearFilters}
            className="border-0 rounded-none"
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Kỳ</th>
                  <th className="px-4 py-3.5">Thời gian</th>
                  <th className="px-4 py-3.5">Ngày chi trả</th>
                  <th className="px-4 py-3.5">Số dòng</th>
                  <th className="px-4 py-3.5">Tổng Gross</th>
                  <th className="px-4 py-3.5">Thực chi</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((p) => (
                  <tr key={p.id} onClick={() => navigate(`/payroll/${p.id}`)} className="hover:bg-slate-50 cursor-pointer">
                    <td className="px-4 py-3.5 font-bold text-slate-900">{String(p.month).padStart(2, '0')}/{p.year}</td>
                    <td className="px-4 py-3.5 text-xs">{formatDate(p.startDate)} – {formatDate(p.endDate)}</td>
                    <td className="px-4 py-3.5">{formatDate(p.payDate)}</td>
                    <td className="px-4 py-3.5">{p.lineCount}{p.needsReviewCount > 0 && <span className="text-amber-600 text-xs ml-1">({p.needsReviewCount} cần rà soát)</span>}</td>
                    <td className="px-4 py-3.5">{formatCurrency(p.totalGross)}</td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900">{formatCurrency(p.totalPayable)}</td>
                    <td className="px-4 py-3.5">
                      <StatusBadge status={p.status} />
                      {p.status === 'LOCKED' && <StatusBadge status={p.paymentStatus} className="ml-2" />}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal isOpen={open} title="Tạo kỳ lương" onClose={() => setOpen(false)}>
        <form onSubmit={create} className="space-y-4">
          {formError && <ErrorMessage message={formError} />}
          <div className="grid grid-cols-2 gap-4">
            <InputField label="Tháng" type="number" min="1" max="12" value={form.month} onChange={(e) => setForm({ ...form, month: e.target.value })} required />
            <InputField label="Năm" type="number" min="2020" max="2100" value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} required />
          </div>
          <InputField label="Ngày chi trả (tùy chọn)" type="date" value={form.payDate} onChange={(e) => setForm({ ...form, payDate: e.target.value })}
            helperText="Để trống: mặc định ngày 05 tháng sau, lùi về ngày làm việc liền trước nếu trùng cuối tuần/lễ." />
          <div className="flex justify-end gap-3">
            <Button variant="outline" type="button" onClick={() => setOpen(false)}>Hủy</Button>
            <Button type="submit" isLoading={busy}>Tạo kỳ lương</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default PayrollListPage;
