import React, { useState } from 'react';
import { Pencil, Plus } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import adminApi from '../../api/adminApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import { formatDate, todayIso } from '../../utils/formatDate';

const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';

export function ApprovalRoutesPage() {
  const { isHR } = useAuth();
  const [tab, setTab] = useState('routes');
  const routes = useAsync(() => adminApi.routes(), []);
  const absences = useAsync(() => adminApi.unavailability(), []);
  const staff = useAsync(() => employeeApi.list({ page: 0, size: 200 }), []);
  const [editing, setEditing] = useState(null);
  const [absenceOpen, setAbsenceOpen] = useState(false);

  const people = (staff.data?.content || []).filter((e) => e.status !== 'TERMINATED');
  const options = people.map((e) => ({ value: e.id, label: `${e.code} – ${e.fullName}` }));

  const routeColumns = [
    { key: 'requesterName', header: 'Người gửi đơn', render: (r) => (<><p className="font-semibold text-slate-900">{r.requesterName}</p><p className="text-xs text-slate-500">{r.requesterCode}</p></>) },
    { key: 'primaryApproverName', header: 'Người duyệt chính', render: (r) => r.primaryApproverName || <span className="text-amber-600">Chưa có</span> },
    { key: 'backupApproverName', header: 'Dự phòng', render: (r) => (r.backupApproverName ? (<><p>{r.backupApproverName}</p><p className="text-xs text-slate-500">QĐ: {r.backupDesignationRef}</p></>) : '-') },
    { key: 'effectiveFrom', header: 'Hiệu lực từ', render: (r) => formatDate(r.effectiveFrom) },
    { key: 'note', header: 'Ghi chú', render: (r) => r.note || '-' },
    { key: 'actions', header: '', align: 'right', render: (r) => <Button size="sm" variant="outline" icon={Pencil} onClick={() => setEditing(r)}>Sửa</Button> },
  ];

  const absenceColumns = [
    { key: 'employeeName', header: 'Người duyệt vắng mặt' },
    { key: 'range', header: 'Thời gian', render: (a) => `${formatDate(a.fromDate)} – ${formatDate(a.toDate)}` },
    { key: 'reason', header: 'Lý do' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Tuyến duyệt</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Người duyệt đơn nghỉ phép/OT của từng nhân viên. Người dự phòng chỉ có hiệu lực khi được chỉ định trước bằng quyết định
            và người duyệt chính đã khai báo vắng mặt.
          </p>
        </div>
        {isHR && tab === 'absence' && <Button icon={Plus} onClick={() => setAbsenceOpen(true)}>Khai báo vắng mặt</Button>}
      </div>

      <Tabs tabs={[{ key: 'routes', label: 'Tuyến duyệt', count: routes.data?.length }, { key: 'absence', label: 'Người duyệt vắng mặt', count: absences.data?.length }]} active={tab} onChange={setTab} />

      <div className={box}>
        {tab === 'routes'
          ? <DataTable columns={routeColumns} rows={routes.data} loading={routes.loading} error={routes.error} onRetry={routes.reload} emptyTitle="Chưa có tuyến duyệt" />
          : <DataTable columns={absenceColumns} rows={absences.data} loading={absences.loading} error={absences.error} onRetry={absences.reload} emptyTitle="Chưa có khai báo vắng mặt" />}
      </div>

      <FormModal
        isOpen={Boolean(editing)}
        title={`Tuyến duyệt – ${editing?.requesterName || ''}`}
        fields={[
          { name: 'primaryApproverId', label: 'Người duyệt chính', type: 'select', options },
          { name: 'backupApproverId', label: 'Người duyệt dự phòng', type: 'select', options, half: true },
          { name: 'backupDesignationRef', label: 'Quyết định chỉ định dự phòng', half: true, helperText: 'Bắt buộc khi có người dự phòng.' },
          { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
          { name: 'note', label: 'Ghi chú / căn cứ', required: true, half: true },
        ]}
        initial={{
          primaryApproverId: editing?.primaryApproverId ?? '', backupApproverId: editing?.backupApproverId ?? '',
          backupDesignationRef: editing?.backupDesignationRef ?? '', effectiveFrom: todayIso(), note: '',
        }}
        onClose={() => setEditing(null)}
        onSubmit={async (v) => {
          await adminApi.updateRoute(editing.requesterId, {
            primaryApproverId: v.primaryApproverId ? Number(v.primaryApproverId) : null,
            backupApproverId: v.backupApproverId ? Number(v.backupApproverId) : null,
            backupDesignationRef: v.backupDesignationRef.trim() || undefined,
            effectiveFrom: v.effectiveFrom, note: v.note.trim(),
          });
          routes.reload();
        }}
      />

      <FormModal
        isOpen={absenceOpen}
        title="Khai báo người duyệt vắng mặt"
        fields={[
          { name: 'employeeId', label: 'Người duyệt', type: 'select', options, required: true },
          { name: 'fromDate', label: 'Từ ngày', type: 'date', required: true, half: true },
          { name: 'toDate', label: 'Đến ngày', type: 'date', required: true, half: true },
          { name: 'reason', label: 'Lý do', required: true },
        ]}
        initial={{ fromDate: todayIso(), toDate: todayIso() }}
        onClose={() => setAbsenceOpen(false)}
        onSubmit={async (v) => {
          await adminApi.createUnavailability({ employeeId: Number(v.employeeId), fromDate: v.fromDate, toDate: v.toDate, reason: v.reason.trim() });
          absences.reload();
        }}
      />
    </div>
  );
}

export default ApprovalRoutesPage;
