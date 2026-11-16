import httpClient from './httpClient';

export const incomeApi = {
  items: (params) => httpClient.get('/income-items', params),
  createItem: (data) => httpClient.post('/income-items', data),
  classify: (id, data) => httpClient.post(`/income-items/${id}/classify`, data),
  confirmItem: (id, note) => httpClient.post(`/income-items/${id}/confirm`, { note }),
  rejectItem: (id, note) => httpClient.post(`/income-items/${id}/reject`, { note }),

  sales: (params) => httpClient.get('/sales-revenues', params),
  createSale: (data) => httpClient.post('/sales-revenues', data),
  confirmOwner: (id, note) => httpClient.post(`/sales-revenues/${id}/confirm-owner`, { note }),
  confirmPayment: (id, note) => httpClient.post(`/sales-revenues/${id}/confirm-payment`, { note }),
  rejectSale: (id, note) => httpClient.post(`/sales-revenues/${id}/reject`, { note }),
};

export default incomeApi;
