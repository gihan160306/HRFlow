import httpClient from './httpClient';

export const payrollApi = {
  getPayrollPeriods: async () => {
    return httpClient.get('/payroll/periods');
  },

  getPayrollPeriodById: async (id) => {
    return httpClient.get(`/payroll/periods/${id}`);
  },

  calculatePayroll: async (periodId) => {
    return httpClient.post(`/payroll/calculate`, { periodId });
  },

  confirmPayroll: async (periodId) => {
    return httpClient.post(`/payroll/periods/${periodId}/approve`);
  },
};

export default payrollApi;
