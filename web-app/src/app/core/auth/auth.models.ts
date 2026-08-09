export type UserRole = 'USER' | 'ADMIN';

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  role: UserRole;
}

export interface AuthSession extends LoginResponse {
  expiresAt: number;
}
