import React from 'react';
import { TrendingUp, TrendingDown } from 'lucide-react';

export function StatCard({
  title,
  value,
  change,
  isPositive = true,
  icon: Icon,
  color = 'blue',
  subtitle,
}) {
  const COLOR_MAP = {
    blue: 'bg-blue-50 text-blue-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    purple: 'bg-purple-50 text-purple-600',
    rose: 'bg-rose-50 text-rose-600',
  };

  return (
    <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className="text-2xl font-bold text-slate-900 mt-1">{value}</p>
        </div>
        {Icon && (
          <div className={`p-3 rounded-xl ${COLOR_MAP[color] || COLOR_MAP.blue}`}>
            <Icon className="w-6 h-6" />
          </div>
        )}
      </div>

      {(change !== undefined || subtitle) && (
        <div className="mt-4 flex items-center text-xs font-medium">
          {change !== undefined && (
            <span
              className={`flex items-center mr-2 ${
                isPositive ? 'text-emerald-600' : 'text-rose-600'
              }`}
            >
              {isPositive ? (
                <TrendingUp className="w-3.5 h-3.5 mr-0.5" />
              ) : (
                <TrendingDown className="w-3.5 h-3.5 mr-0.5" />
              )}
              {change}
            </span>
          )}
          {subtitle && <span className="text-slate-400">{subtitle}</span>}
        </div>
      )}
    </div>
  );
}

export default StatCard;
