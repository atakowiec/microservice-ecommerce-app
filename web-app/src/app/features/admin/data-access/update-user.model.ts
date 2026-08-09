import {UserRole} from "../../../core/auth/auth.models";

export interface UpdateUserModel {
  id: number
  username: string;
  email: string;
  password: string;
  role: UserRole;
}