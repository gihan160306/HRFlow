import { STORAGE_KEYS } from '../../utils/constants';

export function getStoredToken() {
  return localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
}

export function setStoredToken(token) {
  if (token) {
    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
  } else {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
  }
}

export function isAuthenticated() {
  return Boolean(getStoredToken());
}

export function hasRequiredRole(userRole, allowedRoles = []) {
  if (!allowedRoles || allowedRoles.length === 0) return true;
  return allowedRoles.includes(userRole);
}
