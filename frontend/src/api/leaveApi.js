
import httpClient from './httpClient';

export const leaveApi = {
  preview: (data) => httpClient.post('/leave-requests/preview', data),

  // submit=true: tạo và gửi duyệt ngay; false: lưu nháp.
  create: (data, submit = true) =>
    httpClient.request('/leave-requests', {
      method: 'POST',
      body: data,
      params: { submit }
    }),

  mine: () => httpClient.get('/leave-requests/me'),

  inbox: () => httpClient.get('/leave-requests/inbox'),

  all: (params) => httpClient.get('/leave-requests', params),

  get: (id) => httpClient.get(`/leave-requests/${id}`),

  submit: (id) => httpClient.post(`/leave-requests/${id}/submit`),

  cancel: (id) => httpClient.post(`/leave-requests/${id}/cancel`),

  requestCancel: (id, reason) =>
    httpClient.post(`/leave-requests/${id}/cancel-request`, {
      reason
    }),

  // BUG-11: Gửi thêm lý do khi người dự phòng duyệt thay.
  approve: (id, reason) =>
    httpClient.post(`/leave-requests/${id}/approve`, {
      reason,
      backupReason: reason
    }),

  // BUG-11: Gửi thêm lý do khi người dự phòng từ chối thay.
  reject: (id, reason) =>
    httpClient.post(`/leave-requests/${id}/reject`, {
      reason,
      backupReason: reason
    }),

  myBalance: (year) =>
    httpClient.get('/leave-balances/me', { year }),
};

export default leaveApi;
