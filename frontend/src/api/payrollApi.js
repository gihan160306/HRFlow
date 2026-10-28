import httpClient from './httpClient';

export const payrollApi = {
  getPayrollPeriods: async () => {
    return httpClient.get('/payroll/periods');
  },

  getPayrollPeriodById: async (id) => {
    return httpClient.get(`/payroll/periods/${id}`);
  },

  calculatePayroll: async (periodId, employees) => {
    return httpClient.post('/payroll/calculate', { periodId, employees });
  },

  confirmPayroll: async (periodId) => {
    return httpClient.post(`/payroll/periods/${periodId}/approve`);
  },

  resetPayroll: async (periodId, reason) => httpClient.post(`/payroll/periods/${periodId}/reset`, { reason }),

  lockPayroll: async (periodId) => httpClient.post(`/payroll/periods/${periodId}/lock`),
  publishPayslips: async (periodId) => httpClient.post(`/payroll/periods/${periodId}/publish`),
};

export default payrollApi;
