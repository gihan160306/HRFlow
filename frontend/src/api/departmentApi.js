import httpClient from './httpClient';

export const departmentApi = {
  getDepartments: async () => {
    return httpClient.get('/departments');
  },

  getDepartmentById: async (id) => {
    return httpClient.get(`/departments/${id}`);
  },

  createDepartment: async (data) => {
    return httpClient.post('/departments', data);
  },

  updateDepartment: async (id, data) => {
    return httpClient.patch(`/departments/${id}`, data);
  },

  deleteDepartment: async (id) => {
    return httpClient.delete(`/departments/${id}`);
  },
};

export default departmentApi;
