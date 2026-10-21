import httpClient from './httpClient';

export const leaveApi = {
  getLeaveRequests: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/pto-requests${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  getMyLeaves: async () => {
    return httpClient.get('/pto-requests/mine');
  },

  createLeaveRequest: async (data) => {
    return httpClient.post('/pto-requests', data);
  },

  submitLeaveRequest: async (id) => {
    return httpClient.post(`/pto-requests/${id}/submit`);
  },

  approveLeaveRequest: async (id, reason = '') => {
    return httpClient.patch(`/pto-requests/${id}`, {
      action: 'APPROVE',
      reason,
    });
  },

  rejectLeaveRequest: async (id, reason = '') => {
    return httpClient.patch(`/pto-requests/${id}`, {
      action: 'REJECT',
      reason,
    });
  },
};

export default leaveApi;
