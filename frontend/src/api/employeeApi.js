import httpClient from './httpClient';

export const employeeApi = {
  list: (params) => httpClient.get('/employees', params),
  options: () => httpClient.get('/employees/options'),
  get: (id) => httpClient.get(`/employees/${id}`),
  create: (data) => httpClient.post('/employees', data),
  update: (id, data) => httpClient.patch(`/employees/${id}`, data),
  onboardingCheck: (id) => httpClient.get(`/employees/${id}/onboarding-check`),
  activate: (id) => httpClient.post(`/employees/${id}/activate`),
  terminate: (id, data) => httpClient.post(`/employees/${id}/terminate`, data),
  myProfile: () => httpClient.get('/me/profile'),
  updateMyProfile: (data) => httpClient.patch('/me/profile', data),
};

export default employeeApi;
