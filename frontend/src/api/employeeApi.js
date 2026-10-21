import httpClient from './httpClient';

export const employeeApi = {
  getEmployees: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/employees${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  getEmployeeById: async (id) => {
    return httpClient.get(`/employees/${id}`);
  },

  createEmployee: async (data) => {
    return httpClient.post('/employees', data);
  },

  updateEmployee: async (id, data) => {
    return httpClient.put(`/employees/${id}`, data);
  },

  deleteEmployee: async (id) => {
    return httpClient.delete(`/employees/${id}`);
  },
};

export default employeeApi;
