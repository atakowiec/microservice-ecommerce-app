import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { AuthService } from './auth.service';
import { adminGuard, adminLoginGuard } from './auth.guard';

describe('admin route guards', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getAccessToken', 'hasRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
  });

  it('redirects an already authenticated admin away from the login page', () => {
    auth.getAccessToken.and.returnValue('admin-token');
    auth.hasRole.and.returnValue(true);

    const result = runGuard(adminLoginGuard, '/admin/login');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/admin');
  });

  it('allows an authenticated administrator to access protected admin pages', () => {
    auth.getAccessToken.and.returnValue('admin-token');
    auth.hasRole.and.returnValue(true);

    expect(runGuard(adminGuard, '/admin')).toBeTrue();
  });

  it('redirects an unauthenticated visitor to admin login', () => {
    auth.getAccessToken.and.returnValue(null);
    auth.hasRole.and.returnValue(false);

    const result = runGuard(adminGuard, '/admin');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/admin/login?returnUrl=%2Fadmin',
    );
  });

  it('redirects a non-admin away from a product update URL and retains the return URL', () => {
    auth.getAccessToken.and.returnValue('user-token');
    auth.hasRole.and.returnValue(false);

    const result = runGuard(adminGuard, '/admin/products/product-1');

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/admin/login?returnUrl=%2Fadmin%2Fproducts%2Fproduct-1',
    );
  });

  function runGuard(
    guard: typeof adminGuard | typeof adminLoginGuard,
    url: string,
  ): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      guard(
        {} as ActivatedRouteSnapshot,
        { url } as RouterStateSnapshot,
      ) as boolean | UrlTree,
    );
  }
});
