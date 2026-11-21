import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCheck, Bell } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import { notificationApi } from '../../api/recordsApi';
import Button from '../../components/common/Button';
import Pagination from '../../components/common/Pagination';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatDateTime } from '../../utils/formatDate';
import { emitNotificationsChanged } from '../../utils/notificationEvents';

const SIZE = 20;

export function NotificationsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState('');
  const { data, loading, error, reload } = useAsync(() => notificationApi.list({ page, size: SIZE }), [page]);
  const prefs = useAsync(() => notificationApi.preferences(), []);

  const run = async (fn) => {
    setActionError('');
    try { await fn(); reload(); } catch (err) { setActionError(err.message); }
  };

  const markAllRead = () => run(async () => {
    await notificationApi.readAll();
    emitNotificationsChanged();
  });

  const open = async (n) => {
    if (!n.readAt) {
      await notificationApi.markRead(n.id).then(emitNotificationsChanged).catch(() => {});
    }
    if (n.link && n.link.startsWith('/')) navigate(n.link);
    else reload();
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Thông báo</h1>
          <p className="text-sm text-slate-500 mt-0.5">Thông báo về đơn nghỉ phép, OT, kỳ lương và tài khoản.</p>
        </div>
        <Button variant="outline" icon={CheckCheck} onClick={markAllRead}>Đánh dấu đã đọc tất cả</Button>
      </div>

      {actionError && <ErrorMessage message={actionError} />}

      {prefs.data && (
        <label className="flex items-center text-sm text-slate-700 bg-white border border-slate-200/80 rounded-xl px-4 py-3 w-fit">
          <input
            type="checkbox"
            className="rounded border-slate-300 text-blue-600 mr-2"
            checked={Boolean(prefs.data.emailEnabled)}
            onChange={(e) => run(() => notificationApi.savePreferences(e.target.checked).then(prefs.reload))}
          />
          Gửi thêm thông báo qua email
        </label>
      )}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? <Loading /> : error ? <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
          : !data?.content?.length ? <EmptyState title="Bạn chưa có thông báo nào" />
            : (
              <ul className="divide-y divide-slate-100">
                {data.content.map((n) => (
                  <li key={n.id}>
                    <button type="button" onClick={() => open(n)} className={`w-full text-left px-5 py-4 flex gap-3 hover:bg-slate-50 ${n.readAt ? '' : 'bg-blue-50/40'}`}>
                      <Bell className={`w-4 h-4 mt-1 shrink-0 ${n.readAt ? 'text-slate-300' : 'text-blue-600'}`} />
                      <span className="flex-1 min-w-0">
                        <span className={`block text-sm ${n.readAt ? 'text-slate-700' : 'font-bold text-slate-900'}`}>{n.title}</span>
                        {n.message && <span className="block text-sm text-slate-500 mt-0.5">{n.message}</span>}
                        <span className="block text-xs text-slate-400 mt-1">{formatDateTime(n.createdAt)}</span>
                      </span>
                      {!n.readAt && <span className="w-2 h-2 rounded-full bg-blue-600 mt-2 shrink-0" />}
                    </button>
                  </li>
                ))}
              </ul>
            )}
        {data && <Pagination currentPage={page + 1} totalPages={data.totalPages} totalItems={data.totalElements} pageSize={SIZE} onPageChange={(p) => setPage(p - 1)} />}
      </div>
    </div>
  );
}

export default NotificationsPage;
