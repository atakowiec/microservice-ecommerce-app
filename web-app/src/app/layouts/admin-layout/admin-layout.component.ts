import { Component, inject } from '@angular/core';
import {IsActiveMatchOptions, Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  public readonly isActiveMatchOptions: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    matrixParams: 'ignored',
    fragment: 'ignored'
  }

  signOut(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
