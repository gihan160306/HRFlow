import httpClient from './httpClient';

// Hồ sơ có hiệu lực của nhân viên (HR ghi; HR/quyền liên quan đọc).
export const recordsApi = {
  salaries: (id) => httpClient.get(`/employees/${id}/salaries`),
  addSalary: (id, data) => httpClient.post(`/employees/${id}/salaries`, data),
  allowances: (id) => httpClient.get(`/employees/${id}/allowances`),
  addAllowance: (id, data) => httpClient.post(`/employees/${id}/allowances`, data),
  endAllowance: (allowanceId, data) => httpClient.post(`/employee-allowances/${allowanceId}/end`, data),
  assignments: (id) => httpClient.get(`/employees/${id}/assignments`),
  addAssignment: (id, data) => httpClient.post(`/employees/${id}/assignments`, data),
  dependents: (id) => httpClient.get(`/employees/${id}/dependents`),
  addDependent: (id, data) => httpClient.post(`/employees/${id}/dependents`, data),
  updateDependent: (depId, data) => httpClient.put(`/dependents/${depId}`, data),
  insurance: (id) => httpClient.get(`/employees/${id}/insurance-profiles`),
  addInsurance: (id, data) => httpClient.post(`/employees/${id}/insurance-profiles`, data),
  taxProfiles: (id) => httpClient.get(`/employees/${id}/tax-profiles`),
  addTaxProfile: (id, data) => httpClient.post(`/employees/${id}/tax-profiles`, data),
  schedules: (id) => httpClient.get(`/employees/${id}/schedules`),
  addSchedule: (id, data) => httpClient.post(`/employees/${id}/schedules`, data),
};

// Danh mục & cấu hình.
export const configApi = {
  allowanceTypes: () => httpClient.get('/allowance-types'),
  classifyAllowanceType: (id, data) => httpClient.patch(`/allowance-types/${id}`, data),
  workSchedules: () => httpClient.get('/work-schedules'),
  leaveTypes: () => httpClient.get('/leave-types'),
  taxBrackets: () => httpClient.get('/tax-brackets'),
  holidays: (params) => httpClient.get('/holidays', params),
  addHoliday: (data) => httpClient.post('/holidays', data),
  deleteHoliday: (id) => httpClient.delete(`/holidays/${id}`),
  parameters: (category) => httpClient.get('/parameters', category ? { category } : undefined),
  createParameter: (data) => httpClient.post('/parameters', data),
};

export const reportApi = {
  headcount: (params) => httpClient.get('/reports/headcount', params),
  exportHeadcount: (params) => httpClient.download('/reports/headcount/export', params, 'bao-cao-nhan-su.csv'),
  otHours: (year) => httpClient.get('/reports/ot-hours', year ? { year } : undefined),
  exportOtHours: (year) => httpClient.download('/reports/ot-hours/export', year ? { year } : undefined, `bao-cao-gio-ot-${year || new Date().getFullYear()}.csv`),
  payrollFund: (params) => httpClient.get('/reports/payroll-fund', params),
  exportPayrollFund: (params) => httpClient.download('/reports/payroll-fund/export', params, 'bao-cao-quy-luong.csv'),
};

export const notificationApi = {
  list: (params) => httpClient.get('/notifications', params),
  markRead: (id) => httpClient.post(`/notifications/${id}/read`),
  readAll: () => httpClient.post('/notifications/read-all'),
  preferences: () => httpClient.get('/notifications/preferences'),
  savePreferences: (emailEnabled) => httpClient.put('/notifications/preferences', { emailEnabled }),
};

export default recordsApi;
