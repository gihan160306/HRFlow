import httpClient from './httpClient';

/** API của máy chấm công tại quầy – chỉ tài khoản thiết bị KIOSK gọi được (SRS FR-07, DEC-10). */
export const kioskApi = {
  me: () => httpClient.get('/kiosk/me'),
  scan: (qrPayload) => httpClient.post('/kiosk/scan', { qrPayload }),
  punch: (scanId, photoBase64, photoMime = 'image/jpeg') => httpClient.post('/kiosk/punch', { scanId, photoBase64, photoMime }),
};

export default kioskApi;
