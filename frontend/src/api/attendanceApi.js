import httpClient from './httpClient';

export const attendanceApi = {
  today: () => httpClient.get('/attendance/today'),
  checkIn: (data) => httpClient.post('/attendance/check-in', data ?? {}),
  checkOut: (data) => httpClient.post('/attendance/check-out', data ?? {}),
  list: (params) => httpClient.get('/attendance', params),
  timesheet: (params) => httpClient.get('/attendance/timesheet', params),
  review: (params) => httpClient.get('/attendance/review', params),
  confirm: (id, note) => httpClient.post(`/attendance/${id}/confirm`, { note }),
  adjustments: (params) => httpClient.get('/attendance/adjustments', params),
  createAdjustment: (data) => httpClient.post('/attendance/adjustments', data),
  approveAdjustment: (id, note) => httpClient.post(`/attendance/adjustments/${id}/approve`, { note }),
  rejectAdjustment: (id, note) => httpClient.post(`/attendance/adjustments/${id}/reject`, { note }),
  points: () => httpClient.get('/attendance-points'),
  qr: (pointId) => httpClient.get('/attendance/qr', { pointId }),
};

export default attendanceApi;
