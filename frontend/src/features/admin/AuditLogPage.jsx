import React, { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import adminApi from '../../api/adminApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import Pagination from '../../components/common/Pagination';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import { formatDateTime } from '../../utils/formatDate';

const SIZE = 20;
const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';

function JsonBlock({ label, value }) {
  if (!value) return null;
  let text = value;
  try { text = JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2); } catch { /* giữ nguyên */ }
  return (
    <div>
      <p className="text-[11px] font-bold uppercase text-slate-400 mb-1">{label}</p>
      <pre className="text-xs bg-slate-50 border border-slate-200 rounded-lg p-2 overflow-x-auto max-h-48">{text}</pre>
    </div>
  );
}

export function AuditLogPage() {
  const [tab, setTab] = useState('audit');
  const [filters, setFilters] = useState({ action: '', entityType: '', result: '', from: '', to: '' });
  const [applied, setApplied] = useState(filters);
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(null);

  const logs = useAsync(() => adminApi.auditLogs({ ...applied, page, size: SIZE }), [applied, page], { enabled: tab === 'audit' });
  const jobs = useAsync(() => adminApi.jobRuns(), [], { enabled: tab === 'jobs' });

  const apply = (e) => { e.preventDefault(); setPage(0); setApplied(filters); };

  const columns = [
    { key: 'toggle', header: '', render: (l) => (
      <button type="button" aria-label="Chi tiết" onClick={() => setOpen(open === l.id ? null : l.id)} className="text-slate-400 hover:text-slate-700">
        {open === l.id ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
      </button>
    ) },
    { key: 'createdAt', header: 'Thời điểm', render: (l) => formatDateTime(l.createdAt) },
    { key: 'actorName', header: 'Người thực hiện', render: (l) => l.actorName || 'Hệ thống' },
    { key: 'action', header: 'Hành động', render: (l) => <span className="font-mono text-xs">{l.action}</span> },
    { key: 'entity', header: 'Đối tượng', render: (l) => `${l.entityType || '-'}${l.entityId ? ` #${l.entityId}` : ''}` },
    { key: 'result', header: 'Kết quả', render: (l) => (
      <span className={`inline-block whitespace-nowrap px-2 py-0.5 rounded-full text-xs font-semibold border ${l.result === 'SUCCESS' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
        {l.result === 'SUCCESS' ? 'Thành công' : 'Bị từ chối'}
      </span>
    ) },
    { key: 'reason', header: 'Lý do / ghi chú', render: (l) => (
      <div>
        <p>{l.reason || '-'}</p>
        {open === l.id && (
          <div className="mt-2 space-y-2">
            <JsonBlock label="Trước" value={l.beforeJson} />
            <JsonBlock label="Sau" value={l.afterJson} />
            {l.ipAddress && <p className="text-xs text-slate-400">IP: {l.ipAddress}</p>}
          </div>
        )}
      </div>
    ) },
  ];

  const jobColumns = [
    { key: 'startedAt', header: 'Bắt đầu', render: (j) => formatDateTime(j.startedAt) },
    { key: 'jobName', header: 'Tác vụ' },
    { key: 'runKey', header: 'Khóa chạy', render: (j) => j.runKey || '-' },
    { key: 'result', header: 'Kết quả' },
    { key: 'message', header: 'Thông báo', render: (j) => j.message || '-' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Nhật ký hệ thống</h1>
        <p className="text-sm text-slate-500 mt-0.5">Ghi nhận mọi thay đổi và các thao tác bị từ chối. Admin không xem giá trị trước/sau của dữ liệu lương.</p>
      </div>
      <Tabs tabs={[{ key: 'audit', label: 'Nhật ký kiểm toán' }, { key: 'jobs', label: 'Tác vụ nền' }]} active={tab} onChange={setTab} />

      {tab === 'audit' && (
        <form onSubmit={apply} className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 items-end">
          <InputField id="al-action" label="Hành động" placeholder="vd. CREATE_ACCOUNT" value={filters.action} onChange={(e) => setFilters({ ...filters, action: e.target.value.trim() })} />
          <InputField id="al-entity" label="Đối tượng" placeholder="vd. user_account" value={filters.entityType} onChange={(e) => setFilters({ ...filters, entityType: e.target.value.trim() })} />
          <SelectField id="al-result" label="Kết quả" placeholder="Tất cả" options={[{ value: 'SUCCESS', label: 'Thành công' }, { value: 'DENIED', label: 'Bị từ chối' }]} value={filters.result} onChange={(e) => setFilters({ ...filters, result: e.target.value })} />
          <InputField id="al-from" label="Từ ngày" type="date" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
          <InputField id="al-to" label="Đến ngày" type="date" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
          <Button type="submit">Lọc</Button>
        </form>
      )}

      <div className={box}>
        {tab === 'audit' ? (
          <>
            <DataTable columns={columns} rows={logs.data?.content} loading={logs.loading} error={logs.error} onRetry={logs.reload} emptyTitle="Không có bản ghi phù hợp" />
            {logs.data && (
              <Pagination currentPage={page + 1} totalPages={logs.data.totalPages} totalItems={logs.data.totalElements} pageSize={SIZE} onPageChange={(p) => setPage(p - 1)} />
            )}
          </>
        ) : (
          <DataTable columns={jobColumns} rows={jobs.data} loading={jobs.loading} error={jobs.error} onRetry={jobs.reload} emptyTitle="Chưa có lần chạy nào" />
        )}
      </div>
    </div>
  );
}

export default AuditLogPage;
