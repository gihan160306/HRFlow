import React, { useState, useEffect } from 'react';
import { FileCheck2, Plus, Calendar, AlertCircle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import contractApi from '../../api/contractApi';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import EmptyState from '../../components/common/EmptyState';
import ContractForm from './ContractForm';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

const INITIAL_CONTRACTS = [
  {
    id: 1,
    contractNumber: 'HDLD-2021-001',
    employeeName: 'Đặng Minh Hiếu',
    contractType: 'Không xác định thời hạn',
    startDate: '2021-01-10',
    endDate: null,
    baseSalary: 45000000,
    status: 'ACTIVE',
  },
  {
    id: 2,
    contractNumber: 'HDLD-2022-045',
    employeeName: 'Hồ Thị Gia Hân',
    contractType: 'Không xác định thời hạn',
    startDate: '2022-03-01',
    endDate: null,
    baseSalary: 32000000,
    status: 'ACTIVE',
  },
  {
    id: 3,
    contractNumber: 'HDLD-2025-102',
    employeeName: 'Nguyễn Hoàng Phương Ngân',
    contractType: 'Xác định thời hạn (36 tháng)',
    startDate: '2025-06-01',
    endDate: '2028-05-31',
    baseSalary: 28000000,
    status: 'ACTIVE',
  },
  {
    id: 4,
    contractNumber: 'HDLD-2026-015',
    employeeName: 'Trần Thị Lệ Hoa',
    contractType: 'Không xác định thời hạn',
    startDate: '2023-08-15',
    endDate: null,
    baseSalary: 38000000,
    status: 'ACTIVE',
  },
  {
    id: 5,
    contractNumber: 'HDLD-2026-088',
    employeeName: 'Hồ Vinh Quan',
    contractType: 'Thử việc (2 tháng)',
    startDate: '2026-09-01',
    endDate: '2026-10-31',
    baseSalary: 22000000,
    status: 'ACTIVE',
  },
];

export function ContractListPage() {
  const { isHR, isAdmin } = useAuth();
  const canManage = isHR || isAdmin;

  const [contracts, setContracts] = useState(INITIAL_CONTRACTS);
  const [isLoading, setIsLoading] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);

  useEffect(() => {
    async function loadData() {
      try {
        setIsLoading(true);
        const data = await contractApi.getContracts();
        if (Array.isArray(data) && data.length > 0) {
          setContracts(data);
        }
      } catch (err) {
        // Fallback to initial
      } finally {
        setIsLoading(false);
      }
    }
    loadData();
  }, []);

  const handleCreateContract = (newContract) => {
    const item = {
      id: Date.now(),
      ...newContract,
      status: 'ACTIVE',
    };
    setContracts((prev) => [item, ...prev]);
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Quản lý Hợp đồng Lao động
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Theo dõi hợp đồng, thời hạn và cảnh báo gia hạn
          </p>
        </div>

        {canManage && (
          <Button onClick={() => setIsFormOpen(true)} icon={Plus}>
            Tạo hợp đồng
          </Button>
        )}
      </div>

      {/* Contract Alert */}
      <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl flex items-start space-x-3 text-amber-800">
        <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
        <div className="text-sm">
          <span className="font-bold">Cảnh báo thời hạn:</span> Có 1 hợp đồng thử việc sắp kết thúc trong vòng 15 ngày tới (Hồ Vinh Quan - kết thúc 31/10/2026). Vui lòng đánh giá để ký tiếp hợp đồng chính thức.
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {isLoading ? (
          <Loading message="Đang tải danh sách hợp đồng..." />
        ) : contracts.length === 0 ? (
          <EmptyState
            icon={FileCheck2}
            title="Chưa có hợp đồng nào"
            description="Hãy tạo hợp đồng lao động mới cho nhân viên."
            actionLabel="Tạo hợp đồng"
            onAction={() => setIsFormOpen(true)}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Số HĐ & Nhân viên</th>
                  <th className="px-4 py-3.5">Loại hợp đồng</th>
                  <th className="px-4 py-3.5">Hiệu lực</th>
                  <th className="px-4 py-3.5">Lương thỏa thuận</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {contracts.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3.5">
                      <p className="font-bold text-slate-900">{c.contractNumber}</p>
                      <p className="text-xs text-slate-500">{c.employeeName}</p>
                    </td>
                    <td className="px-4 py-3.5 font-medium text-slate-700">
                      {c.contractType}
                    </td>
                    <td className="px-4 py-3.5 text-xs text-slate-500">
                      <div>Từ: {formatDate(c.startDate)}</div>
                      <div>Đến: {c.endDate ? formatDate(c.endDate) : 'Vô thời hạn'}</div>
                    </td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900">
                      {formatCurrency(c.baseSalary)}
                    </td>
                    <td className="px-4 py-3.5">
                      <StatusBadge status={c.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ContractForm
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleCreateContract}
      />
    </div>
  );
}

export default ContractListPage;
