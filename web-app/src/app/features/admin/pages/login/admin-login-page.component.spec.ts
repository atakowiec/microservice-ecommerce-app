import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthSession } from '../../../../core/auth/auth.models';
import { AuthService } from '../../../../core/auth/auth.service';
import { AdminLoginPageComponent } from './admin-login-page.component';

describe('AdminLoginPageComponent', () => {
  const adminSession: AuthSession = {
    accessToken: 'admin-token',
    tokenType: 'Bearer',
    expiresIn: 3_600,
    expiresAt: Date.now() + 3_600_000,
    username: 'admin@example.com',
    role: 'ADMIN',
  };

  let fixture: ComponentFixture<AdminLoginPageComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'logout']);

    await TestBed.configureTestingModule({
      imports: [AdminLoginPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}) },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(AdminLoginPageComponent);
    fixture.detectChanges();
  });

  it('displays the admin login form with username and password fields', () => {
    expect(fixture.debugElement.query(By.css('#username'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('#password'))).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Log in');
  });

  it('redirects to the admin dashboard after valid admin credentials are submitted', () => {
    auth.login.and.returnValue(of(adminSession));
    const navigateSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);

    submitCredentials('admin@example.com', 'correct-password');

    expect(auth.login).toHaveBeenCalledOnceWith({
      username: 'admin@example.com',
      password: 'correct-password',
    });
    expect(navigateSpy).toHaveBeenCalledOnceWith('/admin');
  });

  it('shows a generic error for invalid credentials without identifying an incorrect field', () => {
    auth.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401 })),
    );

    submitCredentials('admin@example.com', 'wrong-password');

    const alert = fixture.debugElement.query(By.css('[role="alert"]')).nativeElement as HTMLElement;
    expect(alert.textContent?.trim()).toBe('The username or password is incorrect.');
  });

  it('shows unlock instructions when the account is locked', () => {
    auth.login.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 423, error: { code: 'ACCOUNT_LOCKED' } }),
      ),
    );

    submitCredentials('locked-admin@example.com', 'password');

    const alert = fixture.debugElement.query(By.css('[role="alert"]')).nativeElement as HTMLElement;
    expect(alert.textContent).toContain('account is locked');
    expect(alert.textContent).toContain('Contact an administrator to unlock it');
  });

  function submitCredentials(username: string, password: string): void {
    const usernameInput = fixture.debugElement.query(By.css('#username')).nativeElement as HTMLInputElement;
    const passwordInput = fixture.debugElement.query(By.css('#password')).nativeElement as HTMLInputElement;
    const submit = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement as HTMLButtonElement;

    usernameInput.value = username;
    usernameInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    submit.click();
    fixture.detectChanges();
  }
});
