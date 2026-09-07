import { BACKEND_URL } from './runtime-config';

export const API_ROUTES = {
  auth: {
    login: `${BACKEND_URL}/user/auth/login`,
  },
  admin: {
    users: `${BACKEND_URL}/user/admin/users`,
    products: `${BACKEND_URL}/catalog/admin/products`,
    categories: `${BACKEND_URL}/catalog/admin/categories`,
  },
} as const;
