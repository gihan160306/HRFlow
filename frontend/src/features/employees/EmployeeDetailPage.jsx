import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Pencil, CheckCircle2, AlertTriangle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import employeeApi from '../../api/employeeApi';
import contractApi from '../../api/contractApi';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import { formatDate } from '../../utils/formatDate';
import { EMPLOYEE_TYPES, WAGE_REGIONS, GENDERS, CONTRACT_TYPES } from '../../utils/constants';

const label = (list, v) => list.find((x) => x.value === v)?.label || v || '-';

function Row({ name, value }) {
  return (
    <div className="py-2.5 grid grid-cols-3 gap-3 border-b border-slate-100 last:border-0">
      <dt className="text-sm text-slate-500">{name}</dt>
      <dd className="col-span-2 text-sm font-medium text-slate-900 break-words">{value || '-'}</dd>
    </div>
  );
}

const VISIBILITY_NOTE = {
  BASIC: 'Bạn chỉ xem được thông tin cơ bản của nhân viên thuộc team (không gồm dữ liệu cá nhân nhạy cảm).',
  MINIMAL: 'Tài khoản quản trị chỉ xem được thông tin định danh tối thiểu.',
};

export function EmployeeDetailPage({ self = false }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isHR, user } = useAuth();
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState(false);
  const [selfOpen, setSelfOpen] = useState(false);
  const [selfForm, setSelfForm] = useState({ phone: '', personalEmail: '', address: '' });

  const { data, loading, error, reload } = useAsync(
    () => (self ? employeeApi.myProfile() : employeeApi.get(id)),
    [self, id]
  );
  const empId = data?.summary?.id;
  const showHrTools = isHR && !self && data?.visibility === 'FULL';

  const { data: check, reload: reloadCheck } = useAsync(
    () => employeeApi.onboardingCheck(empId),
    [empId],
    { enabled: showHrTools && data?.summary?.status === 'ONBOARDING' }
  );
  const { data: contracts } = useAsync(
    () => contractApi.list({ employeeId: empId }),
    [empId],
    { enabled: showHrTools }
  );

  if (loading) return <Loading message="Đang tải hồ sơ..." />;
  if (error) {
    return (
      <div className="space-y-4">
        <Button variant="outline" icon={ArrowLeft} onClick={() => navigate(-1)}>Quay lại</Button>
        <ErrorMessage message={error.status === 404 ? 'Không tìm thấy nhân viên hoặc bạn không có quyền xem.' : error.message} onRetry={reload} />
      </div>
    );
  }

  const s = data.summary;
  const full = data.visibility === 'FULL';

  const activate = async () => {
    setBusy(true);
    setActionError('');
    try {
      await employeeApi.activate(s.id);
      reload();
    } catch (err) {
      setActionError(err.message);
      reloadCheck();
    } finally {
      setBusy(false);
    }
  };

  const openSelfEdit = () => {
    setSelfForm({ phone: s.phone || '', personalEmail: data.personalEmail || '', address: data.address || '' });
    setActionError('');
    setSelfOpen(true);
  };

  const saveSelf = async (e) => {
    e.preventDefault();
    setBusy(true);
    setActionError('');
    try {
      await employeeApi.updateMyProfile({
        phone: selfForm.phone || undefined,
        personalEmail: selfForm.personalEmail || undefined,
        address: selfForm.address || undefined,
      });
      setSelfOpen(false);
      reload();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {!self && <Button variant="outline" size="sm" icon={ArrowLeft} onClick={() => navigate('/employees')}>Danh sách</Button>}
          <div>
            <h1 className="text-2xl font-extrabold text-slate-900">{s.fullName}</h1>
            <p className="text-sm text-slate-500">{s.code} • {s.jobTitleName} • {s.departmentName}</p>
          </div>
          <StatusBadge status={s.status} />
        </div>
        <div className="flex gap-2">
          {self && user?.employeeId && <Button variant="outline" icon={Pencil} onClick={openSelfEdit}>Cập nhật liên hệ</Button>}
          {isHR && !self && s.status !== 'TERMINATED' && (
            <Button icon={Pencil} onClick={() => navigate(`/employees/${s.id}/edit`)}>Chỉnh sửa hồ sơ</Button>
          )}
        </div>
      </div>

      {VISIBILITY_NOTE[data.visibility] && (
        <p className="text-sm text-slate-600 bg-slate-100 rounded-lg p-3">{VISIBILITY_NOTE[data.visibility]}</p>
      )}
      {actionError && <ErrorMessage message={actionError} />}

      {showHrTools && s.status === 'ONBOARDING' && check && (
        <div className={`rounded-2xl border p-5 ${check.ready ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'}`}>
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="font-bold text-slate-900 flex items-center gap-2">
                {check.ready ? <CheckCircle2 className="w-5 h-5 text-emerald-600" /> : <AlertTriangle className="w-5 h-5 text-amber-600" />}
                {check.ready ? 'Hồ sơ onboarding đã đủ điều kiện kích hoạt' : 'Hồ sơ onboarding còn thiếu'}
              </h3>
              {check.missing?.length > 0 && (
                <ul className="mt-2 list-disc pl-5 text-sm text-slate-700 space-y-0.5">
                  {check.missing.map((m) => <li key={m}>{m}</li>)}
                </ul>
              )}
              {check.warnings?.length > 0 && (
                <ul className="mt-2 list-disc pl-5 text-sm text-amber-800 space-y-0.5">
                  {check.warnings.map((m) => <li key={m}>{m}</li>)}
                </ul>
              )}
            </div>
            <Button onClick={activate} isLoading={busy} disabled={!check.ready}>Kích hoạt nhân viên</Button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5">
          <h3 className="text-base font-bold text-slate-800 mb-2">Thông tin công việc</h3>
          <dl>
            <Row name="Mã nhân viên" value={s.code} />
            <Row name="Email công việc" value={s.workEmail} />
            <Row name="Phòng ban" value={s.departmentName} />
            <Row name="Chức danh" value={s.jobTitleName} />
            <Row name="Quản lý trực tiếp" value={s.managerName} />
            <Row name="Loại nhân sự" value={label(EMPLOYEE_TYPES, s.employeeType)} />
            <Row name="Ngày vào làm" value={s.hireDate && formatDate(s.hireDate)} />
            {s.terminationDate && <Row name="Ngày nghỉ việc" value={formatDate(s.terminationDate)} />}
            {full && <Row name="Nơi làm việc" value={data.workLocation} />}
            {full && <Row name="Vùng lương tối thiểu" value={label(WAGE_REGIONS, data.wageRegion)} />}
            {full && data.terminationReason && <Row name="Lý do nghỉ việc" value={data.terminationReason} />}
            {isHR && !self && <Row name="Tài khoản hệ thống" value={s.hasAccount ? 'Đã có' : 'Chưa có (cần yêu cầu Admin khởi tạo)'} />}
          </dl>
        </section>

        {data.visibility !== 'MINIMAL' && (
          <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5">
            <h3 className="text-base font-bold text-slate-800 mb-2">Thông tin cá nhân</h3>
            <dl>
              <Row name="Điện thoại" value={s.phone} />
              {full && <Row name="Giới tính" value={label(GENDERS, data.gender)} />}
              {full && <Row name="Ngày sinh" value={data.dateOfBirth && formatDate(data.dateOfBirth)} />}
              {full && <Row name="Email cá nhân" value={data.personalEmail} />}
              {full && <Row name="Địa chỉ" value={data.address} />}
              {full && <Row name="CCCD/CMND" value={data.nationalId} />}
              {full && <Row name="Ngân hàng" value={data.bankName} />}
              {full && <Row name="Số tài khoản" value={data.bankAccountNo} />}
            </dl>
          </section>
        )}
      </div>

      {showHrTools && contracts && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <h3 className="text-base font-bold text-slate-800">Hợp đồng lao động</h3>
            <Button size="sm" variant="outline" onClick={() => navigate('/contracts')}>Quản lý hợp đồng</Button>
          </div>
          {contracts.length === 0 ? (
            <p className="p-5 text-sm text-slate-500">Chưa có hợp đồng.</p>
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500">
                <tr><th className="px-4 py-3">Số HĐ</th><th className="px-4 py-3">Loại</th><th className="px-4 py-3">Hiệu lực</th><th className="px-4 py-3">Trạng thái</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {contracts.map((c) => (
                  <tr key={c.id}>
                    <td className="px-4 py-3 font-semibold text-slate-900">{c.contractNo}</td>
                    <td className="px-4 py-3">{label(CONTRACT_TYPES, c.contractType)}</td>
                    <td className="px-4 py-3">{formatDate(c.startDate)} – {c.endDate ? formatDate(c.endDate) : 'không thời hạn'}</td>
                    <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}

      <Modal isOpen={selfOpen} title="Cập nhật thông tin liên hệ" onClose={() => setSelfOpen(false)}>
        <form onSubmit={saveSelf} className="space-y-4">
          <p className="text-xs text-slate-500">Bạn chỉ được tự cập nhật điện thoại, email cá nhân và địa chỉ. Các trường khác do HR quản lý.</p>
          {actionError && <ErrorMessage message={actionError} />}
          <InputField label="Điện thoại" value={selfForm.phone} onChange={(e) => setSelfForm({ ...selfForm, phone: e.target.value })} />
          <InputField label="Email cá nhân" type="email" value={selfForm.personalEmail} onChange={(e) => setSelfForm({ ...selfForm, personalEmail: e.target.value })} />
          <InputField label="Địa chỉ" value={selfForm.address} onChange={(e) => setSelfForm({ ...selfForm, address: e.target.value })} />
          <div className="flex justify-end gap-3">
            <Button variant="outline" type="button" onClick={() => setSelfOpen(false)}>Hủy</Button>
            <Button type="submit" isLoading={busy}>Lưu</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default EmployeeDetailPage;
