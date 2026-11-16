import React from 'react';

/** Thanh tab đơn giản. tabs: [{ key, label, count? }] */
export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="flex flex-wrap gap-1 border-b border-slate-200">
      {tabs.map((t) => (
        <button
          key={t.key}
          type="button"
          onClick={() => onChange(t.key)}
          className={`px-4 py-2.5 text-sm font-semibold -mb-px border-b-2 transition-colors ${
            active === t.key
              ? 'border-blue-600 text-blue-700'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          {t.label}
          {t.count !== undefined && t.count !== null && (
            <span className="ml-1.5 text-xs bg-slate-100 text-slate-600 rounded-full px-1.5 py-0.5">{t.count}</span>
          )}
        </button>
      ))}
    </div>
  );
}

export default Tabs;
