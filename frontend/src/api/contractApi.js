import httpClient from './httpClient';

export const contractApi = {
  getContracts: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/contracts${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  getContractById: async (id) => {
    return httpClient.get(`/contracts/${id}`);
  },

  createContract: async (data) => {
    return httpClient.post('/contracts', data);
  },

  renewContract: async (id, data) => {
    return httpClient.post(`/contracts/${id}/renew`, data);
  },

  terminateContract: async (id, data) => {
    return httpClient.post(`/contracts/${id}/terminate`, data);
  },
};

export default contractApi;
