import React, { useState } from 'react';
import { Plus, Lock, Unlock, KeyRound, UserCog, ShieldPlus, Ban } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import adminApi from '../../api/adminApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import PromptDialog from '../../components/common/PromptDialog';
import StatusBadge from '../../components/common/StatusBadge';
import ErrorMessage from '../../components/common/ErrorMessage';
import { formatDateTime } from '../../utils/formatDate';
import { ROLE_LABELS, PAYROLL_GRANTS } from '../../utils/constants';

const ROLE_OPTIONS = Object.entries(ROLE_LABELS).map(([value, label]) => ({ value, label }));
const NON_ADMIN_ROLES = ROLE_OPTIONS.filter((r) => r.value !== 'ADMIN');
const GRANT_OPTIONS = Object.values(PAYROLL_GRANTS).map((v) => ({ value: v, label: v }));
const ACCOUNT_STATUS = {
  ACTIVE: { label: 'Hoạt động', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  LOCKED: { label: 'Đã khóa', cls: 'bg-red-50 text-red-700 border-red-200' },
  DISABLED: { label: 'Vô hiệu', cls: 'bg-slate-100 text-slate-600 border-slate-200' },
};
const REQUEST_STATUS = {
  PENDING: { label: 'Chờ xử lý', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  DONE: { label: 'Đã cấp', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { label: 'Từ chối', cls: 'bg-red-50 text-red-700 border-red-200' },
};

function Pill({ map, value }) {
  const c = map[value] || { label: value, cls: 'bg-slate-100 text-slate-600 border-slate-200' };
  return <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold border ${c.cls}`}>{c.label}</span>;
}

const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';

export function AccountsPage() {
  const { isAdmin, isHR } = useAuth();
  const [tab, setTab] = useState(isAdmin ? 'accounts' : 'requests');
  const [notice, setNotice] = useState(null); // { title, text } — mật khẩu tạm chỉ hiển thị một lần

  const accounts = useAsync(() => adminApi.accounts(), [], { enabled: isAdmin });
  const requests = useAsync(() => adminApi.accountRequests(), []);
  const grants = useAsync(() => adminApi.grants(), [], { enabled: isAdmin });
  const people = useAsync(() => employeeApi.options(), [], { enabled: isHR });
  // Admin chỉ xem được định danh tối thiểu của nhân viên: dùng để chọn người chưa có tài khoản.
  const staff = useAsync(() => employeeApi.list({ page: 0, size: 200 }), [], { enabled: isAdmin });

  const [createOpen, setCreateOpen] = useState(false);
  const [createFrom, setCreateFrom] = useState(null); // account request đang xử lý
  const [requestOpen, setRequestOpen] = useState(false);
  const [grantOpen, setGrantOpen] = useState(false);
  const [roleFor, setRoleFor] = useState(null);
  const [prompt, setPrompt] = useState(null); // { title, label, confirmLabel, run(reason) }

  const tabs = [
    ...(isAdmin ? [{ key: 'accounts', label: 'Tài khoản', count: accounts.data?.length }] : []),
    { key: 'requests', label: 'Đề nghị cấp tài khoản', count: requests.data?.filter((r) => r.status === 'PENDING').length },
    ...(isAdmin ? [{ key: 'grants', label: 'Quyền payroll', count: grants.data?.filter((g) => g.active).length }] : []),
  ];

  const askReason = (title, confirmLabel, run, variant = 'primary') => setPrompt({ title, confirmLabel, run, variant });

  const showTemp = (title, res) => setNotice({ title, text: res?.temporaryPassword });

  const accountColumns = [
    { key: 'email', header: 'Tài khoản', render: (a) => (<><p className="font-semibold text-slate-900">{a.email}</p><p className="text-xs text-slate-500">{a.employeeName ? `${a.employeeCode} – ${a.employeeName}` : 'Admin kỹ thuật (không có hồ sơ nhân viên)'}</p></>) },
    { key: 'role', header: 'Vai trò', render: (a) => ROLE_LABELS[a.role] || a.role },
    { key: 'status', header: 'Trạng thái', render: (a) => <Pill map={ACCOUNT_STATUS} value={a.status} /> },
    { key: 'mustChangePassword', header: 'Đổi MK', render: (a) => (a.mustChangePassword ? 'Bắt buộc' : '-') },
    { key: 'lastLoginAt', header: 'Đăng nhập gần nhất', render: (a) => (a.lastLoginAt ? formatDateTime(a.lastLoginAt) : 'Chưa đăng nhập') },
    {
      key: 'actions', header: 'Thao tác', align: 'right', render: (a) => (
        <div className="flex justify-end gap-1.5 flex-wrap">
          {a.status === 'LOCKED'
            ? <Button size="sm" variant="outline" icon={Unlock} onClick={() => askReason(`Mở khóa ${a.email}`, 'Mở khóa', (r) => adminApi.unlockAccount(a.id, r).then(accounts.reload))}>Mở khóa</Button>
            : <Button size="sm" variant="outline" icon={Lock} onClick={() => askReason(`Khóa ${a.email}`, 'Khóa', (r) => adminApi.lockAccount(a.id, r).then(accounts.reload), 'danger')}>Khóa</Button>}
          <Button size="sm" variant="outline" icon={KeyRound} onClick={() => askReason(`Đặt lại mật khẩu ${a.email}`, 'Đặt lại', (r) => adminApi.resetPassword(a.id, r).then((res) => { showTemp(`Mật khẩu tạm của ${a.email}`, res); accounts.reload(); }))}>Đặt lại MK</Button>
          <Button size="sm" variant="outline" icon={UserCog} onClick={() => setRoleFor(a)}>Vai trò</Button>
        </div>
      ),
    },
  ];

  const requestColumns = [
    { key: 'employeeName', header: 'Nhân viên', render: (r) => (<><p className="font-semibold text-slate-900">{r.employeeName}</p><p className="text-xs text-slate-500">{r.employeeCode} • {r.workEmail}</p></>) },
    { key: 'requestedRole', header: 'Vai trò đề nghị', render: (r) => ROLE_LABELS[r.requestedRole] || r.requestedRole },
    { key: 'reason', header: 'Lý do', render: (r) => r.reason || '-' },
    { key: 'requestedByName', header: 'Người đề nghị' },
    { key: 'createdAt', header: 'Ngày tạo', render: (r) => formatDateTime(r.createdAt) },
    { key: 'status', header: 'Trạng thái', render: (r) => (<><Pill map={REQUEST_STATUS} value={r.status} />{r.processNote && <p className="text-xs text-slate-500 mt-1">{r.processNote}</p>}</>) },
    ...(isAdmin ? [{
      key: 'actions', header: 'Xử lý', align: 'right', render: (r) => r.status === 'PENDING' && (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" onClick={() => setCreateFrom(r)}>Cấp tài khoản</Button>
          <Button size="sm" variant="danger" onClick={() => askReason('Từ chối đề nghị', 'Từ chối', (reason) => adminApi.rejectAccountRequest(r.id, reason).then(requests.reload), 'danger')}>Từ chối</Button>
        </div>
      ),
    }] : []),
  ];

  const grantColumns = [
    { key: 'holderName', header: 'Người được cấp', render: (g) => (<><p className="font-semibold text-slate-900">{g.holderName}</p><p className="text-xs text-slate-500">{g.accountEmail}</p></>) },
    { key: 'permission', header: 'Quyền', render: (g) => <span className="font-bold">{g.permission}</span> },
    { key: 'valid', header: 'Hiệu lực', render: (g) => `${formatDateTime(g.validFrom)} → ${g.validTo ? formatDateTime(g.validTo) : 'không thời hạn'}` },
    { key: 'decisionRef', header: 'Căn cứ', render: (g) => (<><p>{g.decisionRef}</p><p className="text-xs text-slate-500">{g.reason}</p></>) },
    { key: 'active', header: 'Trạng thái', render: (g) => (g.active ? <Pill map={{ A: { label: 'Đang hiệu lực', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' } }} value="A" /> : <Pill map={{ R: { label: g.revokedAt ? 'Đã thu hồi' : 'Hết hạn', cls: 'bg-slate-100 text-slate-600 border-slate-200' } }} value="R" />) },
    {
      key: 'actions', header: '', align: 'right', render: (g) => g.active && (
        <Button size="sm" variant="outline" icon={Ban} onClick={() => askReason('Thu hồi quyền payroll', 'Thu hồi', (r) => adminApi.revokeGrant(g.id, r).then(grants.reload), 'danger')}>Thu hồi</Button>
      ),
    },
  ];

  const accountOptions = (accounts.data || []).filter((a) => a.role !== 'ADMIN' && a.status === 'ACTIVE')
    .map((a) => ({ value: a.id, label: `${a.employeeName || a.email} (${a.email})` }));
  const noAccountOptions = (staff.data?.content || [])
    .filter((e) => !e.hasAccount && e.status !== 'TERMINATED')
    .map((e) => ({ value: e.id, label: `${e.code} – ${e.fullName}` }));
  const peopleOptions =(people.data || []).map((p) => ({ value: p.id, label: `${p.code} – ${p.fullName}` }));

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Tài khoản & phân quyền</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {isAdmin
              ? 'Admin cấp/khóa tài khoản và cấp quyền payroll theo quyết định; Admin không được giữ quyền lương.'
              : 'HR đề nghị cấp tài khoản cho nhân viên; Admin là người khởi tạo tài khoản.'}
          </p>
        </div>
        <div className="flex gap-2">
          {isAdmin && tab === 'accounts' && <Button icon={Plus} onClick={() => setCreateOpen(true)}>Tạo tài khoản</Button>}
          {isHR && tab === 'requests' && <Button icon={Plus} onClick={() => setRequestOpen(true)}>Đề nghị cấp tài khoản</Button>}
          {isAdmin && tab === 'grants' && <Button icon={ShieldPlus} onClick={() => setGrantOpen(true)}>Cấp quyền payroll</Button>}
        </div>
      </div>

      {notice && (
        <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900">
          <p className="font-bold">{notice.title}</p>
          <p className="mt-1">Mật khẩu tạm (chỉ hiển thị một lần, người dùng phải đổi khi đăng nhập):</p>
          <code className="inline-block mt-1 px-3 py-1.5 bg-white border border-amber-200 rounded font-mono text-base select-all">{notice.text}</code>
          <div className="mt-2"><Button size="sm" variant="outline" onClick={() => setNotice(null)}>Đã ghi lại</Button></div>
        </div>
      )}

      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      <div className={box}>
        {tab === 'accounts' && <DataTable columns={accountColumns} {...accounts} rows={accounts.data} onRetry={accounts.reload} emptyTitle="Chưa có tài khoản" />}
        {tab === 'requests' && <DataTable columns={requestColumns} {...requests} rows={requests.data} onRetry={requests.reload} emptyTitle="Chưa có đề nghị nào" />}
        {tab === 'grants' && <DataTable columns={grantColumns} {...grants} rows={grants.data} onRetry={grants.reload} emptyTitle="Chưa có quyền payroll nào được cấp" />}
      </div>

      {/* Admin tạo tài khoản (trực tiếp hoặc từ đề nghị của HR) */}
      <FormModal
        isOpen={createOpen || Boolean(createFrom)}
        title={createFrom ? `Cấp tài khoản cho ${createFrom.employeeName}` : 'Tạo tài khoản'}
        description="Để trống mật khẩu để hệ thống tự sinh mật khẩu tạm. Người dùng bắt buộc đổi mật khẩu ở lần đăng nhập đầu."
        fields={[
          ...(createFrom ? [] : [{ name: 'employeeId', label: 'Nhân viên', type: 'select', options: noAccountOptions, placeholder: 'Không gắn nhân viên (Admin kỹ thuật)', helperText: 'Chỉ liệt kê nhân viên chưa có tài khoản.' }]),
          { name: 'email', label: 'Email đăng nhập', type: 'email', required: true, half: true },
          { name: 'role', label: 'Vai trò', type: 'select', options: ROLE_OPTIONS, required: true, half: true },
          { name: 'temporaryPassword', label: 'Mật khẩu tạm (tuỳ chọn)', half: true, helperText: 'Tối thiểu theo chính sách mật khẩu.' },
          { name: 'reason', label: 'Lý do / căn cứ', type: 'textarea', required: true },
        ]}
        initial={createFrom ? { email: createFrom.workEmail, role: createFrom.requestedRole, reason: createFrom.reason || 'Theo đề nghị của HR' } : { role: 'EMPLOYEE' }}
        submitLabel="Tạo tài khoản"
        onClose={() => { setCreateOpen(false); setCreateFrom(null); }}
        onSubmit={async (v) => {
          const res = await adminApi.createAccount({
            employeeId: createFrom ? createFrom.employeeId : (v.employeeId ? Number(v.employeeId) : undefined),
            email: v.email.trim(), role: v.role, reason: v.reason.trim(),
            temporaryPassword: v.temporaryPassword?.trim() || undefined,
            requestId: createFrom?.id,
          });
          showTemp(`Đã tạo tài khoản ${v.email}`, res);
          accounts.reload();
          requests.reload();
        }}
      />

      <FormModal
        isOpen={requestOpen}
        title="Đề nghị cấp tài khoản"
        description="Chỉ nhân viên chưa có tài khoản và chưa có đề nghị đang chờ."
        fields={[
          { name: 'employeeId', label: 'Nhân viên', type: 'select', options: peopleOptions, required: true },
          { name: 'requestedRole', label: 'Vai trò đề nghị', type: 'select', options: NON_ADMIN_ROLES, required: true },
          { name: 'reason', label: 'Lý do', type: 'textarea' },
        ]}
        initial={{ requestedRole: 'EMPLOYEE' }}
        onClose={() => setRequestOpen(false)}
        onSubmit={async (v) => {
          await adminApi.createAccountRequest({ employeeId: Number(v.employeeId), requestedRole: v.requestedRole, reason: v.reason.trim() || undefined });
          requests.reload();
        }}
      />

      <FormModal
        isOpen={grantOpen}
        title="Cấp quyền payroll"
        description="Quyền payroll tách khỏi vai trò; mỗi lần cấp phải có căn cứ (số quyết định)."
        fields={[
          { name: 'accountId', label: 'Tài khoản', type: 'select', options: accountOptions, required: true },
          { name: 'permission', label: 'Quyền', type: 'select', options: GRANT_OPTIONS, required: true, half: true },
          { name: 'decisionRef', label: 'Số quyết định / căn cứ', required: true, half: true },
          { name: 'validFrom', label: 'Hiệu lực từ', type: 'datetime-local', half: true, helperText: 'Để trống = ngay bây giờ.' },
          { name: 'validTo', label: 'Hiệu lực đến', type: 'datetime-local', half: true, helperText: 'Để trống = không thời hạn.' },
          { name: 'reason', label: 'Lý do', type: 'textarea', required: true },
        ]}
        onClose={() => setGrantOpen(false)}
        onSubmit={async (v) => {
          await adminApi.createGrant({
            accountId: Number(v.accountId), permission: v.permission, decisionRef: v.decisionRef.trim(), reason: v.reason.trim(),
            validFrom: v.validFrom ? new Date(v.validFrom).toISOString() : undefined,
            validTo: v.validTo ? new Date(v.validTo).toISOString() : undefined,
          });
          grants.reload();
        }}
      />

      <FormModal
        isOpen={Boolean(roleFor)}
        title={`Đổi vai trò – ${roleFor?.email || ''}`}
        fields={[
          { name: 'role', label: 'Vai trò mới', type: 'select', options: ROLE_OPTIONS, required: true },
          { name: 'reason', label: 'Lý do', type: 'textarea', required: true },
        ]}
        initial={{ role: roleFor?.role }}
        onClose={() => setRoleFor(null)}
        onSubmit={async (v) => { await adminApi.changeRole(roleFor.id, v.role, v.reason.trim()); accounts.reload(); }}
      />

      <PromptDialog
        isOpen={Boolean(prompt)}
        title={prompt?.title}
        confirmLabel={prompt?.confirmLabel}
        confirmVariant={prompt?.variant}
        onClose={() => setPrompt(null)}
        onSubmit={(reason) => prompt.run(reason)}
      />

      {accounts.error && tab === 'accounts' && <ErrorMessage message={accounts.error.message} />}
    </div>
  );
}

export default AccountsPage;
