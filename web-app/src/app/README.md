# Frontend structure

The application uses feature-first folders and lazy-loaded route components.

- `core/` contains API routes, authentication state, the admin route guard and
  the authorization interceptor.
- `features/` contains the minimal public entry, login and admin pages.
- `layouts/` contains the protected admin shell.
- `shared/` contains reusable presentational components and generic pages.

## Route boundaries

- `/` contains only the login link and, for administrators, the admin link.
- `/login` signs a user in and returns to `/`.
- `/admin/login` is the administrator login.
- Every other `/admin` route is protected by `adminGuard` and requires the
  `ADMIN` role returned by the backend.

Authentication is stored in session storage, so closing the browser session
clears the local login. API requests under `/api/` receive the bearer token
through the interceptor. The development proxy forwards these calls to the API
gateway at `http://localhost:8000`.
