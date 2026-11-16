import httpClient from './httpClient';

// Tài khoản, quyền payroll, tuyến duyệt, nhật ký kiểm toán (ADMIN/HR theo từng endpoint).
export const adminApi = {
  accounts: () => httpClient.get('/accounts'),
  createAccount: (data) => httpClient.post('/accounts', data),
  lockAccount: (id, reason) => httpClient.post(`/accounts/${id}/lock`, { reason }),
  unlockAccount: (id, reason) => httpClient.post(`/accounts/${id}/unlock`, { reason }),
  resetPassword: (id, reason) => httpClient.post(`/accounts/${id}/reset-password`, { reason }),
  changeRole: (id, role, reason) => httpClient.patch(`/accounts/${id}/role`, { role, reason }),

  accountRequests: () => httpClient.get('/account-requests'),
  createAccountRequest: (data) => httpClient.post('/account-requests', data),
  processAccountRequest: (id, data) => httpClient.post(`/account-requests/${id}/process`, data ?? {}),
  rejectAccountRequest: (id, reason) => httpClient.post(`/account-requests/${id}/reject`, { reason }),

  grants: () => httpClient.get('/grants'),
  createGrant: (data) => httpClient.post('/grants', data),
  revokeGrant: (id, reason) => httpClient.post(`/grants/${id}/revoke`, { reason }),

  routes: () => httpClient.get('/approval-routes'),
  updateRoute: (requesterId, data) => httpClient.put(`/approval-routes/${requesterId}`, data),
  unavailability: () => httpClient.get('/approver-unavailability'),
  createUnavailability: (data) => httpClient.post('/approver-unavailability', data),

  auditLogs: (params) => httpClient.get('/audit-logs', params),
  jobRuns: () => httpClient.get('/job-runs'),
};

export default adminApi;
