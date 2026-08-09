import {UserRole} from "../../../core/auth/auth.models";

export interface CreateUserModel {
  username: string;
  email: string;
  password: string;
  role: UserRole;
}