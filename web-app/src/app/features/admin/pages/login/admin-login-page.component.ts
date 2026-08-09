import {Component, inject, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';

import {LoginCredentials} from '../../../../core/auth/auth.models';
import {AuthService} from '../../../../core/auth/auth.service';
import {getLoginErrorMessage} from '../../../../core/auth/login-error';
import {getSafeReturnUrl} from '../../../../core/routing/safe-return-url';
import {LoginFormComponent} from '../../../../shared/components/login-form/login-form.component';

@Component({
  selector: 'app-admin-login-page',
  imports: [LoginFormComponent, RouterLink],
  templateUrl: './admin-login-page.component.html',
  styleUrl: './admin-login-page.component.scss',
})
export class AdminLoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly pending = signal(false);
  readonly errorMessage = signal<string | null>(null);

  login(credentials: LoginCredentials): void {
    this.pending.set(true);
    this.errorMessage.set(null);

    this.auth.login(credentials).subscribe({
      next: (session) => {
        console.log({session})
        if (session.role != 'ADMIN') {
          this.auth.logout();
          this.pending.set(false);
          this.errorMessage.set('This account does not have administrator access.');
          return;
        }

        const returnUrl = getSafeReturnUrl(
          this.route.snapshot.queryParamMap.get('returnUrl'),
          '/admin',
        );
        void this.router.navigateByUrl(returnUrl.startsWith('/admin') ? returnUrl : '/admin');
      },
      error: (error: unknown) => {
        this.pending.set(false);
        this.errorMessage.set(getLoginErrorMessage(error));
      },
    });
  }
}
