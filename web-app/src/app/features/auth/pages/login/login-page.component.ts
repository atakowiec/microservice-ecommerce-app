import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { LoginCredentials } from '../../../../core/auth/auth.models';
import { AuthService } from '../../../../core/auth/auth.service';
import { getLoginErrorMessage } from '../../../../core/auth/login-error';
import { getSafeReturnUrl } from '../../../../core/routing/safe-return-url';
import { LoginFormComponent } from '../../../../shared/components/login-form/login-form.component';

@Component({
  selector: 'app-login-page',
  imports: [LoginFormComponent, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly pending = signal(false);
  readonly errorMessage = signal<string | null>(null);

  login(credentials: LoginCredentials): void {
    this.pending.set(true);
    this.errorMessage.set(null);

    this.auth.login(credentials).subscribe({
      next: () => {
        const returnUrl = getSafeReturnUrl(
          this.route.snapshot.queryParamMap.get('returnUrl'),
          '/',
        );
        void this.router.navigateByUrl(returnUrl);
      },
      error: (error: unknown) => {
        this.pending.set(false);
        this.errorMessage.set(getLoginErrorMessage(error));
      },
    });
  }
}
