import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

export function ErrorMessage({
  title = 'Có lỗi xảy ra',
  message,
  onRetry,
  className = '',
}) {
  return (
    <div
      className={`rounded-xl border border-red-200 bg-red-50 p-4 text-red-800 ${className}`}
    >
      <div className="flex items-start space-x-3">
        <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
        <div className="flex-1">
          <h4 className="text-sm font-semibold">{title}</h4>
          {message && <p className="mt-1 text-sm text-red-700">{message}</p>}
          {onRetry && (
            <button
              onClick={onRetry}
              className="mt-2.5 inline-flex items-center text-xs font-semibold text-red-800 hover:text-red-950 underline"
            >
              <RefreshCw className="w-3.5 h-3.5 mr-1" />
              Thử lại
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default ErrorMessage;
