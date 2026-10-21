import React from 'react';

export function Loading({ message = 'Đang tải dữ liệu...', fullPage = false }) {
  const content = (
    <div className="flex flex-col items-center justify-center p-8 space-y-3">
      <div className="relative w-10 h-10">
        <div className="w-10 h-10 rounded-full border-4 border-blue-200 animate-spin border-t-blue-600"></div>
      </div>
      {message && <p className="text-sm font-medium text-slate-500">{message}</p>}
    </div>
  );

  if (fullPage) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        {content}
      </div>
    );
  }

  return content;
}

export default Loading;
