import React, { forwardRef } from 'react';

export const InputField = forwardRef(function InputField(
  {
    label,
    error,
    helperText,
    icon: Icon,
    className = '',
    id,
    required = false,
    ...props
  },
  ref
) {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-slate-700 mb-1"
        >
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}

      <div className="relative rounded-lg shadow-sm">
        {Icon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Icon className="h-5 w-5" />
          </div>
        )}

        <input
          ref={ref}
          id={inputId}
          className={`block w-full rounded-lg border text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            Icon ? 'pl-10' : 'pl-3.5'
          } pr-3.5 py-2.5 ${
            error
              ? 'border-red-300 text-red-900 focus:border-red-500 focus:ring-red-500 bg-red-50/30'
              : 'border-slate-300 text-slate-900 focus:border-blue-500 bg-white'
          }`}
          {...props}
        />
      </div>

      {error ? (
        <p className="mt-1.5 text-xs text-red-600 font-medium">{error}</p>
      ) : helperText ? (
        <p className="mt-1 text-xs text-slate-500">{helperText}</p>
      ) : null}
    </div>
  );
});

export default InputField;
