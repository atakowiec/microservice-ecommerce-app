import {UserRole} from "../../../core/auth/auth.models";

export type RoleFilter = UserRole | 'ALL';

export interface AdminUserFiltersModel {
  query: string;
  role: RoleFilter;
}