import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { ApiResponse } from '../../../../core/api/api-response.model';
import { API_ROUTES } from '../../../../core/api/api.routes';
import { AdminUser } from './admin-user.model';
import { AdminUsersService } from './admin-users.service';

describe('AdminUsersService', () => {
  const registeredUsers: AdminUser[] = [
    {
      id: 1,
      username: 'user',
      email: 'user@example.com',
      role: 'USER',
    },
    {
      id: 2,
      username: 'admin',
      email: 'admin@example.com',
      role: 'ADMIN',
    },
  ];

  let service: AdminUsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminUsersService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AdminUsersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('requests all registered users from the backend', async () => {
    const usersPromise = firstValueFrom(service.searchUsers());

    const request = http.expectOne({
      method: 'GET',
      url: API_ROUTES.admin.users,
    });
    const response: ApiResponse<AdminUser[]> = {
      status: 200,
      message: 'Users retrieved successfully',
      data: registeredUsers,
    };
    request.flush(response);

    expect(await usersPromise).toEqual(registeredUsers);
  });
});
