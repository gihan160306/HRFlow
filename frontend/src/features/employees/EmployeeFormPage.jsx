import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Save } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import employeeApi from '../../api/employeeApi';
import departmentApi, { catalogApi } from '../../api/departmentApi';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import { EMPLOYEE_TYPES, WAGE_REGIONS, GENDERS } from '../../utils/constants';

const EMPTY = {
  code: '', fullName: '', gender: '', dateOfBirth: '', phone: '', personalEmail: '', workEmail: '',
  address: '', nationalId: '', bankName: '', bankAccountNo: '', employeeType: 'FULL_TIME', hireDate: '',
  workLocation: '', wageRegion: '', departmentId: '', jobTitleId: '', managerId: '', scheduleId: '', reason: '',
};

// Backend dùng COALESCE khi cập nhật nên chuỗi rỗng = "không đổi": bỏ khỏi payload.
const clean = (obj) =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== null && v !== undefined));

export function EmployeeFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [version, setVersion] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const { data: depts } = useAsync(() => departmentApi.list(), []);
  const { data: titles } = useAsync(() => catalogApi.jobTitles(), []);
  const { data: schedules } = useAsync(() => catalogApi.workSchedules(), []);
  const { data: managers } = useAsync(() => employeeApi.options(), []);
  const { data: existing, loading, error: loadError } = useAsync(() => employeeApi.get(id), [id], { enabled: isEdit });

  useEffect(() => {
    if (!existing) return;
    const s = existing.summary;
    setVersion(existing.version);
    setForm({
      ...EMPTY,
      code: s.code, fullName: s.fullName, gender: existing.gender || '', dateOfBirth: existing.dateOfBirth || '',
      phone: s.phone || '', personalEmail: existing.personalEmail || '', workEmail: s.workEmail,
      address: existing.address || '', nationalId: existing.nationalId || '', bankName: existing.bankName || '',
      bankAccountNo: existing.bankAccountNo || '', employeeType: s.employeeType, hireDate: s.hireDate || '',
      workLocation: existing.workLocation || '', wageRegion: existing.wageRegion || '',
      departmentId: s.departmentId, jobTitleId: s.jobTitleId, managerId: s.managerId || '',
    });
  }, [existing]);

  const set = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (isEdit && !form.reason.trim()) return setError('Cần nhập lý do cập nhật hồ sơ (ghi vào nhật ký kiểm toán).');
    if (!isEdit && !form.wageRegion) return setError('Phải chọn vùng lương tối thiểu theo nơi làm việc.');
    setSaving(true);
    try {
      if (isEdit) {
        const { code, workEmail, hireDate, departmentId, jobTitleId, managerId, scheduleId, ...editable } = form;
        const saved = await employeeApi.update(id, { ...clean(editable), reason: form.reason.trim(), version });
        navigate(`/employees/${saved.summary.id}`);
      } else {
        const payload = clean({
          ...form,
          departmentId: form.departmentId && Number(form.departmentId),
          jobTitleId: form.jobTitleId && Number(form.jobTitleId),
          managerId: form.managerId && Number(form.managerId),
          scheduleId: form.scheduleId && Number(form.scheduleId),
        });
        delete payload.reason;
        const saved = await employeeApi.create(payload);
        navigate(`/employees/${saved.summary.id}`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (isEdit && loading) return <Loading message="Đang tải hồ sơ..." />;
  if (isEdit && loadError) return <ErrorMessage message={loadError.message} />;

  const titleOptions = (titles || []).filter((t) => !form.departmentId || !t.departmentId || String(t.departmentId) === String(form.departmentId));

  return (
    <form onSubmit={submit} className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" type="button" icon={ArrowLeft} onClick={() => navigate(-1)}>Quay lại</Button>
        <h1 className="text-2xl font-extrabold text-slate-900">{isEdit ? 'Chỉnh sửa hồ sơ nhân viên' : 'Tiếp nhận nhân viên mới'}</h1>
      </div>

      {error && <ErrorMessage message={error} />}
      {!isEdit && (
        <p className="text-sm text-slate-600 bg-slate-100 rounded-lg p-3">
          Nhân viên mới ở trạng thái <b>Đang tiếp nhận</b>. Sau khi bổ sung hợp đồng, lương cơ bản, lịch làm việc, hồ sơ thuế/bảo hiểm và
          tuyến duyệt, HR kích hoạt ở trang chi tiết. Tài khoản đăng nhập do Admin khởi tạo theo yêu cầu.
        </p>
      )}

      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5 grid grid-cols-1 sm:grid-cols-2 gap-4">
        <h3 className="sm:col-span-2 font-bold text-slate-800">Thông tin cá nhân</h3>
        <InputField label="Mã nhân viên" value={form.code} onChange={set('code')} required disabled={isEdit} />
        <InputField label="Họ và tên" value={form.fullName} onChange={set('fullName')} required />
        <SelectField label="Giới tính" value={form.gender} onChange={set('gender')} options={GENDERS} />
        <InputField label="Ngày sinh" type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
        <InputField label="Điện thoại" value={form.phone} onChange={set('phone')} />
        <InputField label="Email cá nhân" type="email" value={form.personalEmail} onChange={set('personalEmail')} />
        <InputField label="CCCD/CMND" value={form.nationalId} onChange={set('nationalId')} />
        <InputField label="Địa chỉ" value={form.address} onChange={set('address')} />
        <InputField label="Ngân hàng" value={form.bankName} onChange={set('bankName')} />
        <InputField label="Số tài khoản" value={form.bankAccountNo} onChange={set('bankAccountNo')} />
      </section>

      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5 grid grid-cols-1 sm:grid-cols-2 gap-4">
        <h3 className="sm:col-span-2 font-bold text-slate-800">Thông tin công việc</h3>
        <InputField label="Email công việc" type="email" value={form.workEmail} onChange={set('workEmail')} required disabled={isEdit} />
        <InputField label="Ngày vào làm" type="date" value={form.hireDate} onChange={set('hireDate')} required disabled={isEdit} />
        <SelectField label="Loại nhân sự" value={form.employeeType} onChange={set('employeeType')} options={EMPLOYEE_TYPES} placeholder={null} required />
        <InputField label="Nơi làm việc" value={form.workLocation} onChange={set('workLocation')} />
        <SelectField label="Vùng lương tối thiểu" value={form.wageRegion} onChange={set('wageRegion')} options={WAGE_REGIONS}
          required={!isEdit} helperText="Xác định theo nơi làm việc, không mặc định theo tên thành phố." />
        {!isEdit && (
          <>
            <SelectField label="Phòng ban" value={form.departmentId} onChange={set('departmentId')} required
              options={(depts || []).filter((d) => d.active).map((d) => ({ value: d.id, label: d.name }))} />
            <SelectField label="Chức danh" value={form.jobTitleId} onChange={set('jobTitleId')} required
              options={titleOptions.map((t) => ({ value: t.id, label: t.name }))} />
            <SelectField label="Quản lý trực tiếp" value={form.managerId} onChange={set('managerId')}
              options={(managers || []).map((m) => ({ value: m.id, label: `${m.code} – ${m.fullName}` }))} />
            <SelectField label="Lịch làm việc" value={form.scheduleId} onChange={set('scheduleId')}
              options={(schedules || []).map((s) => ({ value: s.id, label: s.name }))} />
          </>
        )}
        {isEdit && (
          <p className="sm:col-span-2 text-xs text-slate-500">
            Phòng ban, chức danh, quản lý, lương và lịch làm việc thay đổi theo quyết định có hiệu lực (điều chuyển/điều chỉnh lương), không sửa trực tiếp ở đây.
          </p>
        )}
      </section>

      {isEdit && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5">
          <InputField label="Lý do cập nhật" value={form.reason} onChange={set('reason')} required
            helperText="Ghi vào nhật ký kiểm toán." />
        </section>
      )}

      <div className="flex justify-end gap-3">
        <Button variant="outline" type="button" onClick={() => navigate(-1)}>Hủy</Button>
        <Button type="submit" icon={Save} isLoading={saving}>{isEdit ? 'Lưu thay đổi' : 'Tạo hồ sơ'}</Button>
      </div>
    </form>
  );
}

export default EmployeeFormPage;
