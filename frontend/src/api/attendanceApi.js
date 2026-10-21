import httpClient from './httpClient';

export const attendanceApi = {
  getAttendanceHistory: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/attendance${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  checkIn: async (data = {}) => {
    return httpClient.post('/attendance/check-in', data);
  },

  checkOut: async (data = {}) => {
    return httpClient.post('/attendance/check-out', data);
  },

  requestAdjustment: async (data) => {
    return httpClient.patch('/attendance/adjust', data);
  },
};

export default attendanceApi;
