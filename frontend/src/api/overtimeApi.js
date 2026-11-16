import httpClient from './httpClient';

export const overtimeApi = {
  schedules: () => httpClient.get('/ot-schedules'),
  createSchedule: (data) => httpClient.post('/ot-schedules', data),
  publishSchedule: (id) => httpClient.post(`/ot-schedules/${id}/publish`),
  closeSchedule: (id) => httpClient.post(`/ot-schedules/${id}/close`),
  cancelSchedule: (id) => httpClient.post(`/ot-schedules/${id}/cancel`),
  registrations: () => httpClient.get('/ot-registrations'),
  myRegistrations: () => httpClient.get('/ot-registrations/me'),
  inbox: () => httpClient.get('/ot-registrations/inbox'),
  register: (data) => httpClient.post('/ot-registrations', data),
  approve: (id, data) => httpClient.post(`/ot-registrations/${id}/approve`, data ?? {}),
  reject: (id, data) => httpClient.post(`/ot-registrations/${id}/reject`, data ?? {}),
  cancel: (id) => httpClient.post(`/ot-registrations/${id}/cancel`),
  confirmActual: (id, data) => httpClient.post(`/ot-registrations/${id}/actual`, data),
  hrCheck: (id) => httpClient.post(`/ot-registrations/${id}/hr-check`),
  unscheduled: () => httpClient.get('/ot-unscheduled'),
  createUnscheduled: (data) => httpClient.post('/ot-unscheduled', data),
  confirmUnscheduled: (id, note) => httpClient.post(`/ot-unscheduled/${id}/confirm`, { note }),
  rejectUnscheduled: (id, note) => httpClient.post(`/ot-unscheduled/${id}/reject`, { note }),
};

export default overtimeApi;
