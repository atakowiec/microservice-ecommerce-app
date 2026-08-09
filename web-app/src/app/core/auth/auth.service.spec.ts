import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_ROUTES } from '../api/api.routes';
import { LoginResponse } from './auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const loginResponse: LoginResponse = {
    accessToken: 'admin-token',
    tokenType: 'Bearer',
    expiresIn: 60,
    username: 'admin@example.com',
    role: 'ADMIN',
  };

  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('stores a successful login session for protected requests', () => {
    service.login({ username: 'admin@example.com', password: 'password' }).subscribe();

    const request = http.expectOne(API_ROUTES.auth.login);
    expect(request.request.method).toBe('POST');
    request.flush(loginResponse);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.hasRole('ADMIN')).toBeTrue();
    expect(service.getAccessToken()).toBe('admin-token');
    expect(sessionStorage.getItem('ecommerce.auth.session')).not.toBeNull();
  });

  it('expires an idle session and requires authentication again', () => {
    const nowSpy = spyOn(Date, 'now').and.returnValue(1_000);
    service.login({ username: 'admin@example.com', password: 'password' }).subscribe();
    http.expectOne(API_ROUTES.auth.login).flush(loginResponse);

    nowSpy.and.returnValue(61_001);

    expect(service.getAccessToken()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem('ecommerce.auth.session')).toBeNull();
  });
});
