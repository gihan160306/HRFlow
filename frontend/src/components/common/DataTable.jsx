import React from 'react';
import Loading from './Loading';
import ErrorMessage from './ErrorMessage';
import EmptyState from './EmptyState';

/**
 * Bảng dữ liệu dùng chung, nhận kết quả của useAsync.
 * columns: [{ key, header, render?(row), className?, align? }]
 */
export function DataTable({ columns, rows, loading, error, onRetry, emptyTitle = 'Chưa có dữ liệu', rowKey = 'id' }) {
  if (loading) return <Loading />;
  if (error) return <div className="p-4"><ErrorMessage message={error.message} onRetry={onRetry} /></div>;
  if (!rows?.length) return <EmptyState title={emptyTitle} />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm text-slate-600">
        <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
          <tr>
            {columns.map((c) => (
              <th key={c.key} className={`px-4 py-3 ${c.align === 'right' ? 'text-right' : ''}`}>{c.header}</th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row, i) => (
            <tr key={row[rowKey] ?? i} className="hover:bg-slate-50/80">
              {columns.map((c) => (
                <td key={c.key} className={`px-4 py-3 ${c.align === 'right' ? 'text-right whitespace-nowrap' : ''} ${c.className || ''}`}>
                  {c.render ? c.render(row) : (row[c.key] ?? '-')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default DataTable;
