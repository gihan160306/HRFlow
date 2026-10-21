import React, { forwardRef } from 'react';

export const SelectField = forwardRef(function SelectField(
  {
    label,
    options = [],
    error,
    helperText,
    className = '',
    id,
    placeholder = 'Chọn một mục...',
    required = false,
    ...props
  },
  ref
) {
  const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label
          htmlFor={selectId}
          className="block text-sm font-medium text-slate-700 mb-1"
        >
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}

      <div className="relative rounded-lg shadow-sm">
        <select
          ref={ref}
          id={selectId}
          className={`block w-full rounded-lg border text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 px-3.5 py-2.5 bg-white ${
            error
              ? 'border-red-300 text-red-900 focus:border-red-500 focus:ring-red-500 bg-red-50/30'
              : 'border-slate-300 text-slate-900 focus:border-blue-500'
          }`}
          {...props}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map((opt) => {
            const val = typeof opt === 'object' ? opt.value : opt;
            const lbl = typeof opt === 'object' ? opt.label : opt;
            return (
              <option key={val} value={val}>
                {lbl}
              </option>
            );
          })}
        </select>
      </div>

      {error ? (
        <p className="mt-1.5 text-xs text-red-600 font-medium">{error}</p>
      ) : helperText ? (
        <p className="mt-1 text-xs text-slate-500">{helperText}</p>
      ) : null}
    </div>
  );
});

export default SelectField;
