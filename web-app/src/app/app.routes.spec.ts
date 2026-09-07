import {adminGuard} from './core/auth/auth.guard';
import {routes} from './app.routes';

describe('application routes', () => {
  it('protects the separate admin product detail page with the admin guard', () => {
    const adminRoute = routes.find(route => route.path === 'admin');
    const productDetailRoute = adminRoute?.children?.find(
      route => route.path === 'products/:productId',
    );

    expect(adminRoute?.canActivate).toContain(adminGuard);
    expect(adminRoute?.canActivateChild).toContain(adminGuard);
    expect(productDetailRoute).toBeDefined();
  });
});
