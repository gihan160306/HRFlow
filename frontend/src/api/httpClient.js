import { STORAGE_KEYS } from '../utils/constants';

// Dev: Vite proxy /api -> http://localhost:8080. Docker: nginx proxy /api -> backend.
const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '');

let unauthorizedHandler = null;

/** AuthContext đăng ký hàm này để xóa phiên khi backend trả 401. */
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = data?.code;
    this.details = Array.isArray(data?.details) ? data.details : [];
  }
}

function buildQuery(params) {
  const sp = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') sp.append(k, v);
  });
  const qs = sp.toString();
  return qs ? `?${qs}` : '';
}

class HttpClient {
  getHeaders(extra = {}) {
    const headers = { Accept: 'application/json', ...extra };
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
  }

  async request(endpoint, { method = 'GET', body, params, responseType = 'json', headers } = {}) {
    const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}${buildQuery(params)}`;
    const config = { method, headers: this.getHeaders(headers) };
    if (body !== undefined && body !== null) {
      config.headers['Content-Type'] = 'application/json';
      config.body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(url, config);
    } catch {
      throw new ApiError('Không kết nối được máy chủ. Vui lòng thử lại sau.', 0, null);
    }

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      if (response.status === 401 && !endpoint.startsWith('/auth/login') && unauthorizedHandler) {
        unauthorizedHandler();
      }
      // Rút gọn danh sách lỗi chi tiết (vd. chốt công có thể trả hàng trăm dòng).
      const MAX_DETAILS = 5;
      const shown = (data?.details || []).slice(0, MAX_DETAILS).map((d) => `${d.field}: ${d.message}`);
      const extra = (data?.details?.length || 0) - shown.length;
      const fieldMsg = shown.length ? ` (${shown.join('; ')}${extra > 0 ? `; …và ${extra} mục khác` : ''})` : '';
      throw new ApiError(
        (data?.message || `Yêu cầu thất bại (HTTP ${response.status})`) + fieldMsg,
        response.status,
        data
      );
    }

    if (response.status === 204) return null;
    if (responseType === 'blob') return response.blob();
    return response.json().catch(() => null);
  }

  get(endpoint, params, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET', params });
  }
  post(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'POST', body: body ?? undefined });
  }
  put(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'PUT', body });
  }
  patch(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'PATCH', body });
  }
  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }

  /** Tải file (CSV/PDF) có kèm Authorization. */
  async download(endpoint, params, filename) {
    const blob = await this.request(endpoint, { params, responseType: 'blob' });
    const href = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = href;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(href);
  }

  /** Mở file (PDF) trong tab mới. */
  async openBlob(endpoint, params) {
    const blob = await this.request(endpoint, { params, responseType: 'blob' });
    const href = URL.createObjectURL(blob);
    window.open(href, '_blank', 'noopener');
    setTimeout(() => URL.revokeObjectURL(href), 60000);
  }
}

export const httpClient = new HttpClient();
export default httpClient;
