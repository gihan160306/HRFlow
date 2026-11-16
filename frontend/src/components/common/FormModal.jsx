import React, { useState, useEffect } from 'react';
import Modal from './Modal';
import Button from './Button';
import InputField from './InputField';
import SelectField from './SelectField';
import ErrorMessage from './ErrorMessage';

/**
 * Hộp thoại biểu mẫu khai báo theo cấu hình.
 * fields: [{ name, label, type='text'|'number'|'date'|'time'|'datetime-local'|'select'|'textarea'|'checkbox',
 *            options, required, helperText, placeholder, min, max, step, half }]
 * `onSubmit(values)` có thể trả Promise; lỗi hiển thị trong hộp thoại, thành công thì tự đóng.
 * Giá trị rỗng ('') được giữ nguyên - bên gọi tự quy đổi sang undefined/null khi cần.
 */
export function FormModal({ isOpen, title, description, fields, initial = {}, submitLabel = 'Lưu', onSubmit, onClose, maxWidth }) {
  const [values, setValues] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const v = {};
    fields.forEach((f) => {
      v[f.name] = initial[f.name] ?? (f.type === 'checkbox' ? false : '');
    });
    setValues(v);
    setError('');
    setBusy(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const set = (name, value) => setValues((s) => ({ ...s, [name]: value }));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await onSubmit(values);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title={title} onClose={onClose} maxWidth={maxWidth}>
      <form onSubmit={submit} className="space-y-4">
        {description && <p className="text-sm text-slate-600">{description}</p>}
        {error && <ErrorMessage message={error} />}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {fields.map((f) => {
            const id = `ff-${f.name}`;
            const span = f.half ? '' : 'sm:col-span-2';
            if (f.type === 'select') {
              return (
                <div key={f.name} className={span}>
                  <SelectField id={id} label={f.label} required={f.required} options={f.options || []} helperText={f.helperText}
                    placeholder={f.placeholder || 'Chọn một mục...'} value={values[f.name] ?? ''} onChange={(e) => set(f.name, e.target.value)} />
                </div>
              );
            }
            if (f.type === 'textarea') {
              return (
                <div key={f.name} className={span}>
                  <label htmlFor={id} className="block text-sm font-medium text-slate-700 mb-1">
                    {f.label} {f.required && <span className="text-red-500">*</span>}
                  </label>
                  <textarea id={id} rows={3} required={f.required} value={values[f.name] ?? ''} onChange={(e) => set(f.name, e.target.value)}
                    className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                  {f.helperText && <p className="mt-1 text-xs text-slate-500">{f.helperText}</p>}
                </div>
              );
            }
            if (f.type === 'checkbox') {
              return (
                <label key={f.name} className={`flex items-center text-sm text-slate-700 ${span}`}>
                  <input type="checkbox" checked={Boolean(values[f.name])} onChange={(e) => set(f.name, e.target.checked)}
                    className="rounded border-slate-300 text-blue-600 mr-2" />
                  {f.label}
                </label>
              );
            }
            return (
              <div key={f.name} className={span}>
                <InputField id={id} label={f.label} type={f.type || 'text'} required={f.required} helperText={f.helperText}
                  placeholder={f.placeholder} min={f.min} max={f.max} step={f.step}
                  value={values[f.name] ?? ''} onChange={(e) => set(f.name, e.target.value)} />
              </div>
            );
          })}
        </div>
        <div className="flex justify-end gap-3 pt-1">
          <Button variant="outline" type="button" onClick={onClose} disabled={busy}>Hủy</Button>
          <Button type="submit" isLoading={busy}>{submitLabel}</Button>
        </div>
      </form>
    </Modal>
  );
}

export default FormModal;
