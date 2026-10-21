import React from 'react';
import { Calculator, Lock, Send, Download, CheckCircle2 } from 'lucide-react';
import Button from '../../components/common/Button';

export function PayrollActions({
  periodStatus = 'DRAFT',
  onCalculate,
  onApprove,
  onSendPayslips,
  isLoading = false,
}) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      {periodStatus !== 'PAID' && (
        <Button
          variant="primary"
          onClick={onCalculate}
          isLoading={isLoading}
          icon={Calculator}
        >
          Tính toán lương tự động
        </Button>
      )}

      {periodStatus === 'CALCULATED' && (
        <Button
          variant="outline"
          onClick={onApprove}
          icon={Lock}
          className="border-emerald-600 text-emerald-700 hover:bg-emerald-50"
        >
          Duyệt & Khóa bảng lương
        </Button>
      )}

      {(periodStatus === 'APPROVED' || periodStatus === 'PAID') && (
        <Button
          variant="secondary"
          onClick={onSendPayslips}
          icon={Send}
        >
          Gửi email phiếu lương
        </Button>
      )}

      <Button variant="outline" icon={Download}>
        Xuất file Excel
      </Button>
    </div>
  );
}

export default PayrollActions;
