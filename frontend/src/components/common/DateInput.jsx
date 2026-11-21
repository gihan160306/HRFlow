import React, { forwardRef, useEffect, useRef, useState } from 'react';
import { CalendarDays } from 'lucide-react';

// Ô nhập ngày hiển thị dd/mm/yyyy (không phụ thuộc ngôn ngữ trình duyệt).
// Giá trị vào/ra vẫn là ISO yyyy-mm-dd để không phải sửa nơi gọi.

const isoToDisplay = (iso) => {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || '');
  return m ? `${m[3]}/${m[2]}/${m[1]}` : '';
};

const displayToIso = (text) => {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(text);
  if (!m) return null;
  const [, d, mo, y] = m;
  const date = new Date(Number(y), Number(mo) - 1, Number(d));
  if (date.getFullYear() !== Number(y) || date.getMonth() !== Number(mo) - 1 || date.getDate() !== Number(d)) return null;
  return `${y}-${mo}-${d}`;
};

const mask = (raw) => {
  const digits = raw.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
};

export const DateInput = forwardRef(function DateInput(
  // eslint-disable-next-line no-unused-vars
  { type, id, name, value, onChange, min, max, required, disabled, className = '', placeholder = 'dd/mm/yyyy', ...rest },
  ref
) {
  const [text, setText] = useState(isoToDisplay(value));
  const textRef = useRef(null);
  const nativeRef = useRef(null);

  useEffect(() => {
    // Đồng bộ khi giá trị bên ngoài thay đổi (reset form, chọn từ lịch...)
    if (displayToIso(text) !== (value || null) && !(text === '' && !value)) setText(isoToDisplay(value));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const emit = (iso) => onChange?.({ target: { value: iso, name, id }, currentTarget: { value: iso, name, id } });

  const validate = (iso, el = textRef.current) => {
    if (!el) return;
    let msg = '';
    if (el.value && !iso) msg = 'Ngày không hợp lệ (dd/mm/yyyy)';
    else if (iso && min && iso < min) msg = `Ngày phải từ ${isoToDisplay(min)} trở đi`;
    else if (iso && max && iso > max) msg = `Ngày không được sau ${isoToDisplay(max)}`;
    el.setCustomValidity(msg);
  };

  const handleText = (e) => {
    const next = mask(e.target.value);
    setText(next);
    const iso = displayToIso(next);
    validate(iso, e.target);
    if (next === '') emit('');
    else if (iso) emit(iso);
  };

  const handleNative = (e) => {
    const iso = e.target.value;
    setText(isoToDisplay(iso));
    validate(iso);
    emit(iso);
  };

  const openPicker = () => {
    const el = nativeRef.current;
    if (!el || disabled) return;
    try { el.showPicker(); } catch { el.focus(); el.click(); }
  };

  const setRefs = (el) => {
    textRef.current = el;
    if (typeof ref === 'function') ref(el);
    else if (ref) ref.current = el;
  };

  return (
    <>
      <input
        ref={setRefs}
        id={id}
        name={name}
        type="text"
        inputMode="numeric"
        autoComplete="off"
        placeholder={placeholder}
        value={text}
        onChange={handleText}
        onBlur={() => validate(displayToIso(text))}
        required={required}
        disabled={disabled}
        className={`${className} pr-10`}
        {...rest}
      />
      <button
        type="button"
        tabIndex={-1}
        aria-label="Chọn ngày"
        onClick={openPicker}
        disabled={disabled}
        className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 disabled:opacity-50"
      >
        <CalendarDays className="h-4 w-4" />
      </button>
      <input
        ref={nativeRef}
        type="date"
        tabIndex={-1}
        aria-hidden="true"
        value={value || ''}
        min={min}
        max={max}
        onChange={handleNative}
        className="absolute right-0 bottom-0 w-px h-px opacity-0 pointer-events-none"
      />
    </>
  );
});

export default DateInput;
