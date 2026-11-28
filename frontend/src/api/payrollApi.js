import httpClient from './httpClient';

export const payrollApi = {
  periods: () => httpClient.get('/payroll/periods'),
  period: (id) => httpClient.get(`/payroll/periods/${id}`),
  createPeriod: (data) => httpClient.post('/payroll/periods', data),
  precheck: (id) => httpClient.get(`/payroll/periods/${id}/precheck`),
  closeTimesheet: (id) => httpClient.post(`/payroll/periods/${id}/close-timesheet`),
  reopenTimesheet: (id, reason) => httpClient.post(`/payroll/periods/${id}/reopen-timesheet`, { reason }),
  calculate: (id) => httpClient.post(`/payroll/periods/${id}/calculate`),
  reopen: (id, reason) => httpClient.post(`/payroll/periods/${id}/reopen`, { reason }),
  reconcile: (id, note) => httpClient.post(`/payroll/periods/${id}/reconcile`, { note }),
  approve: (id) => httpClient.post(`/payroll/periods/${id}/approve`),
  lock: (id) => httpClient.post(`/payroll/periods/${id}/lock`),
  markPaid: (id, data) => httpClient.post(`/payroll/periods/${id}/mark-paid`, data),
  lines: (id) => httpClient.get(`/payroll/periods/${id}/lines`),
  line: (lineId) => httpClient.get(`/payroll/lines/${lineId}`),
  audit: (id) => httpClient.get(`/payroll/periods/${id}/audit`),
  resolveConflict: (id, data) => httpClient.post(`/payroll/periods/${id}/conflict-resolution`, data),
  adjustments: () => httpClient.get('/payroll/adjustments'),
  createAdjustment: (data) => httpClient.post('/payroll/adjustments', data),
  approveAdjustment: (id, note) => httpClient.post(`/payroll/adjustments/${id}/approve`, { note }),
  rejectAdjustment: (id, note) => httpClient.post(`/payroll/adjustments/${id}/reject`, { note }),
  exportCsv: (id) => httpClient.download(`/payroll/periods/${id}/export`, undefined, `bang-luong-${id}.csv`),
  createTaxBracketVersion: (data) => httpClient.post('/tax-brackets', data),
  updateTaxBracketVersion: (data) => httpClient.put('/tax-brackets', data),
};

export default payrollApi;
