import { HttpErrorResponse } from '@angular/common/http';

export function getLoginErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const errorCode = isErrorPayload(error.error) ? error.error.code : null;

    if (error.status === 423 || errorCode === 'ACCOUNT_LOCKED') {
      return 'Your account is locked. Contact an administrator to unlock it.';
    }

    if (error.status === 0) {
      return 'The sign-in service is unavailable. Please try again shortly.';
    }

    if (error.status === 400 || error.status === 401) {
      return 'The username or password is incorrect.';
    }
  }

  return 'We could not sign you in. Please try again.';
}

function isErrorPayload(value: unknown): value is { code?: string } {
  return typeof value === 'object' && value !== null;
}
