import React, { useEffect } from 'react';
import { X } from 'lucide-react';

export function Modal({ isOpen, title, onClose, children, footer, maxWidth = 'max-w-lg' }) {
  useEffect(() => {
    if (!isOpen) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-start sm:items-center justify-center p-4">
      <div role="dialog" aria-modal="true" aria-label={title}
        className={`bg-white rounded-2xl ${maxWidth} w-full p-6 shadow-xl relative my-8`}>
        <button type="button" onClick={onClose} aria-label="Đóng"
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 rounded-lg">
          <X className="w-5 h-5" />
        </button>
        {title && <h3 className="text-lg font-bold text-slate-900 mb-4 pr-8">{title}</h3>}
        {children}
        {footer && <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-end gap-3">{footer}</div>}
      </div>
    </div>
  );
}

export default Modal;
