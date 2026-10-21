import httpClient from './httpClient';

export const overtimeApi = {
  getOvertimeSchedules: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/overtime/schedules${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  createOvertimeSchedule: async (data) => {
    return httpClient.post('/overtime/schedules', data);
  },

  getOvertimeRegistrations: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    const endpoint = `/overtime/registrations${query ? `?${query}` : ''}`;
    return httpClient.get(endpoint);
  },

  registerOvertime: async (data) => {
    return httpClient.post('/overtime/registrations', data);
  },

  approveOvertime: async (id, reason = '') => {
    return httpClient.patch(`/overtime/registrations/${id}`, {
      status: 'APPROVED',
      rejectionReason: reason,
    });
  },

  rejectOvertime: async (id, reason = '') => {
    return httpClient.patch(`/overtime/registrations/${id}`, {
      status: 'REJECTED',
      rejectionReason: reason,
    });
  },
};

export default overtimeApi;
