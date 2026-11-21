import httpClient from './httpClient';

export const attendanceApi = {
  today: () => httpClient.get('/attendance/today'),
  // QR cá nhân động (15 giây, dùng một lần) để đưa vào máy chấm công tại quầy – SRS FR-07, DEC-10
  myQr: () => httpClient.get('/attendance/my-qr'),
  list: (params) => httpClient.get('/attendance', params),
  timesheet: (params) => httpClient.get('/attendance/timesheet', params),
  review: (params) => httpClient.get('/attendance/review', params),
  confirm: (id, note) => httpClient.post(`/attendance/${id}/confirm`, { note }),
  adjustments: (params) => httpClient.get('/attendance/adjustments', params),
  createAdjustment: (data) => httpClient.post('/attendance/adjustments', data),
  approveAdjustment: (id, note) => httpClient.post(`/attendance/adjustments/${id}/approve`, { note }),
  rejectAdjustment: (id, note) => httpClient.post(`/attendance/adjustments/${id}/reject`, { note }),
  points: () => httpClient.get('/attendance-points'),
  createPoint: (data) => httpClient.post('/attendance-points', data),
  updatePoint: (id, data) => httpClient.put(`/attendance-points/${id}`, data),
  photo: (id, kind) => httpClient.get(`/attendance/${id}/photo`, { kind }, { responseType: 'blob' }),
};

export default attendanceApi;
