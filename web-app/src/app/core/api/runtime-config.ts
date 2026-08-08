declare global {
  interface Window {
    __env?: {
      backendUrl?: string;
    };
  }
}

const configuredBackendUrl = window.__env?.backendUrl?.trim() || '/api';

export const BACKEND_URL = configuredBackendUrl.replace(/\/+$/, '');

export function isBackendRequest(url: string): boolean {
  return url === BACKEND_URL || url.startsWith(`${BACKEND_URL}/`);
}

export {};
