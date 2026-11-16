import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import ErrorMessage from '../../components/common/ErrorMessage';
import { CONTRACT_TYPES } from '../../utils/constants';

const EMPTY = {
  employeeId: '', contractNo: '', contractType: 'FIXED_TERM', startDate: '', endDate: '', signedDate: '',
  baseSalary: '', decisionRef: '', note: '',
};

/** mode: 'create' | 'renew'. Gia hạn: ngày bắt đầu = ngày sau khi HĐ cũ hết hạn (do backend tính). */
export function ContractForm({ isOpen, mode = 'create', contract, employees = [], onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const renew = mode === 'renew';

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setForm(renew ? { ...EMPTY, contractType: contract?.contractType || 'FIXED_TERM' } : EMPTY);
  }, [isOpen, renew, contract]);

  const set = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.contractType !== 'INDEFINITE' && !renew && !form.endDate) {
      return setError('Hợp đồng có thời hạn phải có ngày kết thúc.');
    }
    setSaving(true);
    try {
      const salary = form.baseSalary === '' ? undefined : Number(form.baseSalary);
      if (renew) {
        await onSubmit({
          contractNo: form.contractNo.trim(),
          contractType: form.contractType,
          endDate: form.endDate || undefined,
          baseSalary: salary,
          reason: form.note.trim(),
        });
      } else {
        await onSubmit({
          employeeId: Number(form.employeeId),
          contractNo: form.contractNo.trim(),
          contractType: form.contractType,
          startDate: form.startDate,
          endDate: form.endDate || undefined,
          signedDate: form.signedDate || undefined,
          baseSalary: salary,
          decisionRef: form.decisionRef.trim() || undefined,
          note: form.note.trim() || undefined,
        });
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title={renew ? `Gia hạn hợp đồng ${contract?.contractNo || ''}` : 'Tạo hợp đồng lao động'} onClose={onClose} maxWidth="max-w-xl">
      <form onSubmit={submit} className="space-y-4">
        {error && <ErrorMessage message={error} />}
        {!renew && (
          <SelectField label="Nhân viên" value={form.employeeId} onChange={set('employeeId')} required
            options={employees.map((m) => ({ value: m.id, label: `${m.code} – ${m.fullName}` }))} />
        )}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <InputField label="Số hợp đồng" value={form.contractNo} onChange={set('contractNo')} required />
          <SelectField label="Loại hợp đồng" value={form.contractType} onChange={set('contractType')} options={CONTRACT_TYPES} placeholder={null} required />
          {!renew && <InputField label="Ngày bắt đầu" type="date" value={form.startDate} onChange={set('startDate')} required />}
          <InputField label="Ngày kết thúc" type="date" value={form.endDate} onChange={set('endDate')}
            helperText={form.contractType === 'INDEFINITE' ? 'Để trống với hợp đồng không xác định thời hạn.' : undefined} />
          {!renew && <InputField label="Ngày ký" type="date" value={form.signedDate} onChange={set('signedDate')} />}
          <InputField label={renew ? 'Lương cơ bản mới (VND)' : 'Lương cơ bản (VND)'} type="number" min="0" step="1000"
            value={form.baseSalary} onChange={set('baseSalary')} required={!renew}
            helperText={renew ? 'Bỏ trống nếu giữ nguyên mức lương hiện tại.' : undefined} />
        </div>
        {!renew && <InputField label="Căn cứ / số quyết định" value={form.decisionRef} onChange={set('decisionRef')} />}
        <InputField label={renew ? 'Lý do gia hạn' : 'Ghi chú'} value={form.note} onChange={set('note')} required={renew} />
        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" isLoading={saving}>{renew ? 'Gia hạn' : 'Tạo hợp đồng'}</Button>
        </div>
      </form>
    </Modal>
  );
}

export default ContractForm;
