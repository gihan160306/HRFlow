import httpClient from './httpClient';

export const contractApi = {
  list: (params) => httpClient.get('/contracts', params),
  get: (id) => httpClient.get(`/contracts/${id}`),
  document: (id) => httpClient.get(`/contracts/${id}/document`),
  create: (data) => httpClient.post('/contracts', data),
  renew: (id, data) => httpClient.post(`/contracts/${id}/renew`, data),
  terminate: (id, data) => httpClient.post(`/contracts/${id}/terminate`, data),
};

export default contractApi;
