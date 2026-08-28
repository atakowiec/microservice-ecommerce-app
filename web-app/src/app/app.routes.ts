import {Routes} from '@angular/router';

import {adminGuard, adminLoginGuard} from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Home',
    loadComponent: () =>
      import('./features/public/pages/home/home-page.component').then(
        (component) => component.HomePageComponent,
      ),
  },
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () =>
      import('./features/auth/pages/login/login-page.component').then(
        (component) => component.LoginPageComponent,
      ),
  },
  {
    path: 'admin/login',
    title: 'Admin sign in',
    canActivate: [adminLoginGuard],
    loadComponent: () =>
      import('./features/admin/pages/login/admin-login-page.component').then(
        (component) => component.AdminLoginPageComponent,
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    canActivateChild: [adminGuard],
    loadComponent: () =>
      import('./layouts/admin-layout/admin-layout.component').then(
        (component) => component.AdminLayoutComponent,
      ),
    children: [
      {
        path: '',
        title: 'Admin workspace',
        loadComponent: () =>
          import('./features/admin/pages/dashboard/admin-dashboard-page.component').then(
            (component) => component.AdminDashboardPageComponent,
          ),
      },
      {
        path: 'users',
        title: 'User Management',
        loadComponent: () =>
          import('./features/admin/pages/users/admin-users-page.component').then(
            (component) => component.AdminUsersPageComponent,
          ),
      },
      {
        path: 'products',
        title: 'Products List',
        loadComponent: () =>
          import('./features/admin/pages/products/admin-products-page.component').then(
            (component) => component.AdminProductsPageComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    title: 'Page not found',
    loadComponent: () =>
      import('./shared/pages/not-found/not-found-page.component').then(
        (component) => component.NotFoundPageComponent,
      ),
  },
];
