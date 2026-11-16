import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import ErrorMessage from '../../components/common/ErrorMessage';

const EMPTY = { code: '', name: '', description: '', active: true, decisionRef: '' };

export function DepartmentForm({ isOpen, department, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const isEdit = Boolean(department?.id);

  useEffect(() => {
    if (!isOpen) return;
    setError('');
    setForm(isEdit
      ? { code: department.code, name: department.name, description: department.description || '', active: department.active, decisionRef: '' }
      : EMPTY);
  }, [isOpen, department, isEdit]);

  const set = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await onSubmit({
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        active: form.active,
        decisionRef: form.decisionRef.trim(),
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title={isEdit ? 'Cập nhật phòng ban' : 'Thêm phòng ban'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {error && <ErrorMessage message={error} />}
        <InputField label="Mã phòng ban" value={form.code} onChange={set('code')} required />
        <InputField label="Tên phòng ban" value={form.name} onChange={set('name')} required />
        <InputField label="Mô tả" value={form.description} onChange={set('description')} />
        <InputField label="Số quyết định của Giám đốc" value={form.decisionRef} onChange={set('decisionRef')} required
          helperText="Thay đổi cơ cấu phải có căn cứ quyết định hợp lệ; được ghi vào nhật ký kiểm toán." />
        <label className="flex items-center text-sm text-slate-700">
          <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })}
            className="rounded border-slate-300 text-blue-600 mr-2" />
          Đang hoạt động
        </label>
        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" isLoading={saving}>Lưu</Button>
        </div>
      </form>
    </Modal>
  );
}

export default DepartmentForm;
