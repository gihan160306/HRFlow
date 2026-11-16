import httpClient from './httpClient';

export const departmentApi = {
  list: () => httpClient.get('/departments'),
  create: (data) => httpClient.post('/departments', data),
  update: (id, data) => httpClient.patch(`/departments/${id}`, data),
};

export const catalogApi = {
  jobTitles: () => httpClient.get('/job-titles'),
  workSchedules: () => httpClient.get('/work-schedules'),
  leaveTypes: () => httpClient.get('/leave-types'),
};

export default departmentApi;
