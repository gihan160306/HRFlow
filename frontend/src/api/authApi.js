import httpClient from './httpClient';

export const authApi = {
  login: (email, password) => httpClient.post('/auth/login', { email, password }),
  me: () => httpClient.get('/auth/me'),
  changePassword: (currentPassword, newPassword) =>
    httpClient.post('/auth/change-password', { currentPassword, newPassword }),
  logout: () => httpClient.post('/auth/logout'),
};

export default authApi;
