import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import ErrorMessage from '../../components/common/ErrorMessage';
import { LEAVE_UNITS } from '../../utils/constants';
import { todayIso } from '../../utils/formatDate';
import leaveApi from '../../api/leaveApi';

const EMPTY = () => ({
  leaveTypeId: '', unit: 'FULL_DAY', startDate: todayIso(), endDate: todayIso(), startTime: '08:30', endTime: '10:30',
  reason: '', evidence: '',
});

export function LeaveRequestForm({ isOpen, leaveTypes = [], onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY());
  const [error, setError] = useState('');
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setForm(EMPTY());
      setError('');
      setPreview(null);
    }
  }, [isOpen]);

  const set = (name) => (e) => {
    setForm((f) => ({ ...f, [name]: e.target.value }));
    setPreview(null);
  };

  const type = leaveTypes.find((t) => String(t.id) === String(form.leaveTypeId));
  const hours = form.unit === 'HOURS';
  const single = form.unit !== 'FULL_DAY'; // nửa ngày/theo giờ: trong một ngày

  const payload = () => {
    const endDate = single ? form.startDate : form.endDate;
    return {
      leaveTypeId: Number(form.leaveTypeId),
      unit: form.unit,
      startDate: form.startDate,
      endDate,
      startTime: hours ? form.startTime : undefined,
      endTime: hours ? form.endTime : undefined,
      reason: form.reason.trim() || undefined,
      evidence: form.evidence.trim() || undefined,
    };
  };

  const validate = () => {
    if (!form.leaveTypeId) return 'Vui lòng chọn loại nghỉ.';
    if (!single && form.endDate < form.startDate) return 'Ngày kết thúc phải từ ngày bắt đầu trở đi.';
    if (!form.reason.trim()) return 'Lý do nghỉ là bắt buộc.';
    if (type?.requiresEvidence && !form.evidence.trim()) return 'Loại nghỉ này yêu cầu minh chứng.';
    return '';
  };

  const doPreview = async () => {
    const v = validate();
    if (v) return setError(v);
    setError('');
    setBusy(true);
    try {
      setPreview(await leaveApi.preview(payload()));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const save = async (submit) => {
    const v = validate();
    if (v) return setError(v);
    setError('');
    setBusy(true);
    try {
      await leaveApi.create(payload(), submit);
      onSaved?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Tạo đơn xin nghỉ phép (PTO)" onClose={onClose} maxWidth="max-w-xl">
      <div className="space-y-4">
        {error && <ErrorMessage message={error} />}
        <SelectField label="Loại nghỉ" value={form.leaveTypeId} onChange={set('leaveTypeId')} required
          options={leaveTypes.map((t) => ({ value: t.id, label: `${t.name}${t.paid ? '' : ' (không lương)'}` }))} />
        <SelectField label="Hình thức" value={form.unit} onChange={set('unit')} options={LEAVE_UNITS} placeholder={null} />
        <div className="grid grid-cols-2 gap-4">
          <InputField label={single ? 'Ngày nghỉ' : 'Từ ngày'} type="date" value={form.startDate} onChange={set('startDate')} required />
          {!single && <InputField label="Đến hết ngày" type="date" min={form.startDate} value={form.endDate} onChange={set('endDate')} required />}
        </div>
        {hours && (
          <div className="grid grid-cols-2 gap-4">
            <InputField label="Từ giờ" type="time" value={form.startTime} onChange={set('startTime')} required />
            <InputField label="Đến giờ" type="time" value={form.endTime} onChange={set('endTime')} required />
          </div>
        )}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Lý do <span className="text-red-500">*</span></label>
          <textarea rows={3} value={form.reason} onChange={set('reason')}
            className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <InputField label={`Minh chứng${type?.requiresEvidence ? '' : ' (nếu có)'}`} value={form.evidence} onChange={set('evidence')}
          required={Boolean(type?.requiresEvidence)} helperText={type?.requiresEvidence ? 'Loại nghỉ này yêu cầu minh chứng.' : undefined} />

        {preview && (
          <div className="rounded-lg bg-blue-50 border border-blue-200 p-3 text-sm text-blue-900">
            Tổng thời gian nghỉ: <b>{preview.totalDays} ngày</b> ({preview.totalMinutes} phút làm việc theo lịch, đã loại ngày nghỉ/lễ).
          </div>
        )}

        <div className="flex flex-wrap justify-end gap-3 pt-2 border-t border-slate-100">
          <Button variant="outline" type="button" onClick={onClose}>Đóng</Button>
          <Button variant="outline" type="button" onClick={doPreview} isLoading={busy}>Tính số ngày</Button>
          <Button variant="secondary" type="button" onClick={() => save(false)} isLoading={busy}>Lưu nháp</Button>
          <Button type="button" onClick={() => save(true)} isLoading={busy}>Gửi duyệt</Button>
        </div>
      </div>
    </Modal>
  );
}

export default LeaveRequestForm;
