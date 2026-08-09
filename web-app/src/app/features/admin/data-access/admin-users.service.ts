import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {API_ROUTES} from '../../../core/api/api.routes';
import {AdminUser} from './admin-user.model';
import {AdminUserFiltersModel} from "./admin-user-filters.model";
import {CreateUserModel} from "./create-user.model";
import {UpdateUserModel} from "./update-user.model";

@Injectable({providedIn: 'root'})
export class AdminUsersService {
  private readonly http = inject(HttpClient);

  searchUsers(filters?: AdminUserFiltersModel): Observable<AdminUser[]> {
    let params = new HttpParams();

    const query = filters?.query.trim();

    if (query) {
      params = params.set('query', query);
    }

    if (filters && filters.role !== 'ALL') {
      params = params.set('role', filters.role);
    }

    return this.http.get<AdminUser[]>(
      API_ROUTES.admin.users,
      {params},
    );
  }

  createNewUser(dto: CreateUserModel): Observable<AdminUser> {
    return this.http.post<AdminUser>(
      API_ROUTES.admin.users,
      dto
    )
  }

  patchUser(dto: UpdateUserModel) {
    return this.http.patch<AdminUser>(
      API_ROUTES.admin.users,
      dto
    )
  }

  deleteUser(userId: number): Observable<AdminUser> {
    const params = new HttpParams().set('id', userId);

    return this.http.delete<AdminUser>(
      API_ROUTES.admin.users,
      {params}
    );
  }
}
