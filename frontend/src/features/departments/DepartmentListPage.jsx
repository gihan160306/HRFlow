import React, { useState } from 'react';
import { Plus, Pencil } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import departmentApi from '../../api/departmentApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import DepartmentForm from './DepartmentForm';

export function DepartmentListPage() {
  const { isHR } = useAuth();
  const { data, loading, error, reload } = useAsync(() => departmentApi.list(), []);
  const [editing, setEditing] = useState(null); // null: đóng; {}: tạo mới; {id,...}: sửa

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Phòng ban</h1>
          <p className="text-sm text-slate-500 mt-0.5">Danh mục phòng ban và quy mô nhân sự</p>
        </div>
        {isHR && <Button icon={Plus} onClick={() => setEditing({})}>Thêm phòng ban</Button>}
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Chưa có phòng ban" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Mã</th>
                  <th className="px-4 py-3.5">Tên phòng ban</th>
                  <th className="px-4 py-3.5">Mô tả</th>
                  <th className="px-4 py-3.5">Nhân sự</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  {isHR && <th className="px-4 py-3.5 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((d) => (
                  <tr key={d.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-mono text-xs">{d.code}</td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900">{d.name}</td>
                    <td className="px-4 py-3.5">{d.description || '-'}</td>
                    <td className="px-4 py-3.5">{d.headcount}</td>
                    <td className="px-4 py-3.5">
                      <StatusBadge status={d.active ? 'ACTIVE' : 'INACTIVE'} label={d.active ? 'Hoạt động' : 'Ngừng hoạt động'} />
                    </td>
                    {isHR && (
                      <td className="px-4 py-3.5 text-right">
                        <button onClick={() => setEditing(d)} aria-label="Chỉnh sửa" title="Chỉnh sửa"
                          className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg">
                          <Pencil className="w-4 h-4" />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <DepartmentForm
        isOpen={editing !== null}
        department={editing}
        onClose={() => setEditing(null)}
        onSubmit={async (payload) => {
          if (editing?.id) await departmentApi.update(editing.id, payload);
          else await departmentApi.create(payload);
          setEditing(null);
          reload();
        }}
      />
    </div>
  );
}

export default DepartmentListPage;
