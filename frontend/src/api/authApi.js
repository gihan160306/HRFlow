import httpClient from './httpClient';

export const authApi = {
  login: async (credentials) => {
    return httpClient.post('/auth/login', credentials);
  },

  getCurrentUser: async () => {
    return httpClient.get('/auth/me');
  },

  logout: async () => {
    try {
      await httpClient.post('/auth/logout');
    } catch {
      // Ignored
    } finally {
      localStorage.removeItem('hrflow_auth_token');
      localStorage.removeItem('hrflow_current_user');
    }
  },
};

export default authApi;
