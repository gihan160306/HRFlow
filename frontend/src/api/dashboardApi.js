import httpClient from './httpClient';

export const dashboardApi = {
  summary: () => httpClient.get('/dashboard/summary'),
  unreadCount: () => httpClient.get('/notifications/unread-count'),
  notifications: (params) => httpClient.get('/notifications', params),
  markRead: (id) => httpClient.post(`/notifications/${id}/read`),
  readAll: () => httpClient.post('/notifications/read-all'),
};

export default dashboardApi;
