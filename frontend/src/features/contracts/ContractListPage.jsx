import React, { useState } from 'react';
import { Plus, RefreshCw, XCircle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import contractApi from '../../api/contractApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import SelectField from '../../components/common/SelectField';
import InputField from '../../components/common/InputField';
import PromptDialog from '../../components/common/PromptDialog';
import ContractForm from './ContractForm';
import { formatDate, todayIso } from '../../utils/formatDate';
import { CONTRACT_TYPES } from '../../utils/constants';

const STATUS_FILTER = [
  { value: 'ACTIVE', label: 'Đang hiệu lực' },
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'EXPIRED', label: 'Hết hạn' },
  { value: 'TERMINATED', label: 'Đã chấm dứt' },
];

export function ContractListPage() {
  const { isHR } = useAuth();
  const [status, setStatus] = useState('');
  const [expiring, setExpiring] = useState(false);
  const [form, setForm] = useState(null); // { mode, contract }
  const [terminate, setTerminate] = useState(null);
  const [terminateDate, setTerminateDate] = useState(todayIso());

  const { data, loading, error, reload } = useAsync(
    () => contractApi.list({ status, expiringDays: expiring ? 30 : undefined }),
    [status, expiring]
  );
  const { data: employees } = useAsync(() => employeeApi.options(), [], { enabled: isHR });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Hợp đồng lao động</h1>
          <p className="text-sm text-slate-500 mt-0.5">{isHR ? 'Quản lý hợp đồng toàn công ty' : 'Hợp đồng của bạn'}</p>
        </div>
        {isHR && <Button icon={Plus} onClick={() => setForm({ mode: 'create' })}>Tạo hợp đồng</Button>}
      </div>

      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-sm flex flex-wrap items-end gap-4">
        <SelectField className="w-56" placeholder="Tất cả trạng thái" value={status} onChange={(e) => setStatus(e.target.value)} options={STATUS_FILTER} />
        <label className="flex items-center text-sm text-slate-700 pb-2.5">
          <input type="checkbox" checked={expiring} onChange={(e) => setExpiring(e.target.checked)} className="rounded border-slate-300 text-blue-600 mr-2" />
          Sắp hết hạn trong 30 ngày
        </label>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Không có hợp đồng" description="Không có hợp đồng nào khớp bộ lọc." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Số HĐ</th>
                  {isHR && <th className="px-4 py-3.5">Nhân viên</th>}
                  <th className="px-4 py-3.5">Loại</th>
                  <th className="px-4 py-3.5">Hiệu lực</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  {isHR && <th className="px-4 py-3.5 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-semibold text-slate-900">{c.contractNo}</td>
                    {isHR && <td className="px-4 py-3.5">{c.employeeName}<span className="block text-xs text-slate-400">{c.employeeCode}</span></td>}
                    <td className="px-4 py-3.5">{CONTRACT_TYPES.find((t) => t.value === c.contractType)?.label || c.contractType}</td>
                    <td className="px-4 py-3.5 text-xs">
                      {formatDate(c.startDate)} – {c.endDate ? formatDate(c.endDate) : 'không thời hạn'}
                      {c.daysToExpire !== null && c.daysToExpire !== undefined && c.daysToExpire <= 30 && (
                        <span className="block text-amber-600 font-semibold">Còn {c.daysToExpire} ngày</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5"><StatusBadge status={c.status} /></td>
                    {isHR && (
                      <td className="px-4 py-3.5 text-right whitespace-nowrap">
                        {['ACTIVE', 'EXPIRED'].includes(c.status) && (
                          <Button size="sm" variant="outline" icon={RefreshCw} onClick={() => setForm({ mode: 'renew', contract: c })}>Gia hạn</Button>
                        )}
                        {['ACTIVE', 'DRAFT'].includes(c.status) && (
                          <Button size="sm" variant="ghost" icon={XCircle} className="ml-2 text-red-600"
                            onClick={() => { setTerminateDate(todayIso()); setTerminate(c); }}>Chấm dứt</Button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ContractForm
        isOpen={Boolean(form)}
        mode={form?.mode}
        contract={form?.contract}
        employees={employees || []}
        onClose={() => setForm(null)}
        onSubmit={async (payload) => {
          if (form.mode === 'renew') await contractApi.renew(form.contract.id, payload);
          else await contractApi.create(payload);
          setForm(null);
          reload();
        }}
      />

      <PromptDialog
        isOpen={Boolean(terminate)}
        title={`Chấm dứt hợp đồng ${terminate?.contractNo || ''}`}
        description={
          <span className="block">
            Thao tác không thể hoàn tác.
            <span className="block mt-3">
              <InputField label="Ngày chấm dứt" type="date" value={terminateDate} onChange={(e) => setTerminateDate(e.target.value)} required />
            </span>
          </span>
        }
        label="Lý do chấm dứt"
        confirmLabel="Chấm dứt hợp đồng"
        confirmVariant="danger"
        onClose={() => setTerminate(null)}
        onSubmit={async (reason) => {
          await contractApi.terminate(terminate.id, { terminationDate: terminateDate, reason });
          reload();
        }}
      />
    </div>
  );
}

export default ContractListPage;
