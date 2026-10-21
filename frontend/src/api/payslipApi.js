import httpClient from './httpClient';

export const payslipApi = {
  getPayslips: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/payslips${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  getPayslipById: async (id) => {
    return httpClient.get(`/payslips/${id}`);
  },

  getMyPayslips: async () => {
    return httpClient.get('/payslips/me');
  },

  sendPayslipEmail: async (id) => {
    return httpClient.post(`/payslips/${id}/send-email`);
  },
};

export default payslipApi;
