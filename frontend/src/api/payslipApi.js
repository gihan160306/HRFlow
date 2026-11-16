import httpClient from './httpClient';

export const payslipApi = {
  mine: () => httpClient.get('/payslips/me'),
  get: (id) => httpClient.get(`/payslips/${id}`),
  viewPdf: (id) => httpClient.openBlob(`/payslips/${id}/pdf`),
  downloadPdf: (id, fileName) =>
    httpClient.download(`/payslips/${id}/pdf`, { download: true }, fileName || `phieu-luong-${id}.pdf`),
};

export default payslipApi;
