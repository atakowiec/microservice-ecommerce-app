import {AsyncPipe} from '@angular/common';
import {Component, inject, signal} from '@angular/core';
import {
  BehaviorSubject,
  catchError,
  filter,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  startWith,
  switchMap,
  take,
} from 'rxjs';

import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ModalComponent} from "../../../../shared/components/modal/modal.component";
import {
  ConfirmationModalComponent
} from "../../../../shared/components/confirmation-modal/confirmation-modal.component";
import {AdminUser} from "../../data-access/user/admin-user.model";
import {AdminUsersService} from "../../data-access/user/admin-users.service";
import {UserRole} from "../../../../core/auth/auth.models";
import {AdminUserFiltersModel, RoleFilter} from "../../data-access/user/admin-user-filters.model";
import {CreateUserModel} from "../../data-access/user/create-user.model";
import {UpdateUserModel} from "../../data-access/user/update-user.model";

type AdminUsersPageState =
  | { status: 'loading'; users: readonly AdminUser[] }
  | { status: 'loaded'; users: readonly AdminUser[] }
  | { status: 'error'; users: readonly AdminUser[] };

@Component({
  selector: 'app-admin-users-page',
  imports: [AsyncPipe, ReactiveFormsModule, ModalComponent, ConfirmationModalComponent],
  templateUrl: './admin-users-page.component.html',
  styleUrl: './admin-users-page.component.scss',
})
export class AdminUsersPageComponent {
  private readonly usersService = inject(AdminUsersService);

  readonly deleteModalOpen = signal(false);
  readonly deleting = signal(false);
  readonly deleteUserId = signal<number | null>(null);
  readonly deleteError = signal<string | null>(null);

  readonly selectedUserId = signal<number | null>(null);
  readonly userFormModalOpen = signal(false);
  readonly userFormSubmitting = signal(false);
  readonly userFormServerError = signal<string | null>(null);

  readonly userDetailsForm = new FormGroup({
    id: new FormControl("0"),
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
      ]
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email,
      ],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
      ]
    }),
    role: new FormControl<UserRole>('USER', {
      nonNullable: true,
      validators: [
        Validators.required,
      ]
    }),
  })

  readonly filtersForm = new FormGroup({
    query: new FormControl('', {
      nonNullable: true,
    }),
    role: new FormControl<RoleFilter>('ALL', {
      nonNullable: true,
    }),
  });

  private readonly submittedFilters$ = new BehaviorSubject<AdminUserFiltersModel>({
    query: '',
    role: 'ALL',
  });

  readonly state$: Observable<AdminUsersPageState> = this.submittedFilters$.pipe(
    switchMap((filters) =>
      this.usersService.searchUsers(filters).pipe(
        map((users) => ({
          status: 'loaded' as const,
          users
        })),
        startWith({
          status: "loading" as const,
          users: []
        }),
        catchError(() => of({
          status: "error" as const,
          users: []
        }))
      ),
    ),
    shareReplay({
      bufferSize: 1,
      refCount: true
    })
  )

  public applyFilters(): void {
    if (this.filtersForm.invalid)
      return;

    this.submittedFilters$.next(
      this.filtersForm.getRawValue()
    )
  }

  openUserFormModal(userId?: number): void {
    this.selectedUserId.set(userId ? userId : null);

    if (userId) {
      this.state$.pipe(
        filter(
          (state): state is Extract<AdminUsersPageState, { status: 'loaded' }> => state.status === 'loaded',
        ),
        take(1),
        map((state) => state.users.find((user) => user.id === userId))
      ).subscribe((user) => {
        if (!user) {
          this.userFormServerError.set(`User with id ${userId} was not found.`,);
          return;
        }

        this.selectedUserId.set(user.id);

        this.userDetailsForm.reset({
          id: String(user.id),
          username: user.username,
          email: user.email,
          password: '',
          role: user.role,
        });
      })
    }

    this.userFormModalOpen.set(true);
  }

  onModalClosed(): void {
    this.userDetailsForm.reset();
  }

  protected submitUserDetailsForm() {
    this.userFormServerError.set(null);
    this.userDetailsForm.markAllAsTouched();

    if (this.userDetailsForm.invalid || this.userFormSubmitting()) {
      return;
    }

    this.userFormSubmitting.set(true);

    if(this.selectedUserId()) {
      this.updateUser()
    } else {
      this.createUser()
    }
  }

  protected openDeleteConfirmation(userId: number): void {
    this.deleteUserId.set(userId);
    this.deleteError.set(null);
    this.deleteModalOpen.set(true);
  }

  protected confirmDelete(): void {
    const userId = this.deleteUserId();

    if (userId === null || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.deleteError.set(null);

    this.usersService.deleteUser(userId)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => {
          this.submittedFilters$.next(this.filtersForm.getRawValue());
          this.deleteModalOpen.set(false);
          this.deleteUserId.set(null);
        },
        error: (error) => {
          if (error.status === 404) {
            this.deleteError.set('The user no longer exists.');
            return;
          }

          this.deleteError.set('The user could not be deleted. Please try again.');
        },
      });
  }

  private createUser() {
    const accountDetails: CreateUserModel = this.userDetailsForm.getRawValue();

    this.usersService.createNewUser(accountDetails)
      .pipe(finalize(() => this.userFormSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.submittedFilters$.next(this.filtersForm.getRawValue())

          this.userFormModalOpen.set(false)
        },

        error: (error) => {
          if (error.status === 409) {
            this.userFormServerError.set(error.error.message);
            return;
          }

          this.userFormServerError.set('The user could not be created. Please try again.');
        },
      });
  }

  private updateUser() {
    const rawValues = this.userDetailsForm.getRawValue();
    const accountDetails: UpdateUserModel = {
      ...rawValues,
      id: this.selectedUserId()!
    };

    this.usersService.patchUser(accountDetails)
      .pipe(finalize(() => this.userFormSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.submittedFilters$.next(this.filtersForm.getRawValue())

          this.userFormModalOpen.set(false)
        },

        error: (error) => {
          if (error.status === 409) {
            this.userFormServerError.set(error.error.message);
            return;
          }

          this.userFormServerError.set('The user could not be saved. Please try again.');
        },
      });
  }
}
