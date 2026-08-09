import { BACKEND_URL } from './runtime-config';

export const API_ROUTES = {
  auth: {
    login: `${BACKEND_URL}/user/auth/login`,
  },
  admin: {
    users: `${BACKEND_URL}/user/admin/users`,
  },
} as const;
