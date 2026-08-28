import { UserRole } from '../../../../core/auth/auth.models';

export interface AdminUser {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}
