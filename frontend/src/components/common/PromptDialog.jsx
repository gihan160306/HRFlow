import React, { useState, useEffect } from 'react';
import Modal from './Modal';
import Button from './Button';
import ErrorMessage from './ErrorMessage';

/**
 * Hộp thoại xác nhận có nhập lý do/ghi chú. `onSubmit(text)` có thể trả Promise;
 * lỗi được hiển thị trong hộp thoại, thành công thì tự đóng.
 */
export function PromptDialog({
  isOpen,
  title,
  description,
  label = 'Lý do',
  required = true,
  confirmLabel = 'Xác nhận',
  confirmVariant = 'primary',
  onSubmit,
  onClose,
}) {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setText('');
      setError('');
      setLoading(false);
    }
  }, [isOpen]);

  const submit = async (e) => {
    e.preventDefault();
    if (required && !text.trim()) {
      setError(`${label} là bắt buộc.`);
      return;
    }
    setLoading(true);
    setError('');
    try {
      await onSubmit(text.trim());
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title={title} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {description && <p className="text-sm text-slate-600">{description}</p>}
        {error && <ErrorMessage message={error} />}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            {label} {required && <span className="text-red-500">*</span>}
          </label>
          <textarea
            rows={3}
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="outline" type="button" onClick={onClose} disabled={loading}>Hủy</Button>
          <Button variant={confirmVariant} type="submit" isLoading={loading}>{confirmLabel}</Button>
        </div>
      </form>
    </Modal>
  );
}

export default PromptDialog;
