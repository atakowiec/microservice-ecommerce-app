// noinspection DuplicatedCode

import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {of} from 'rxjs';

import {AdminUsersPageComponent} from './admin-users-page.component';
import {AdminUsersService} from "../../data-access/user/admin-users.service";
import {AdminUser} from "../../data-access/user/admin-user.model";

describe('AdminUsersPageComponent', () => {
  let registeredUsers: AdminUser[] = []

  let component: AdminUsersPageComponent;
  let fixture: ComponentFixture<AdminUsersPageComponent>;
  let usersService: jasmine.SpyObj<AdminUsersService>;

  beforeEach(async () => {
    registeredUsers = [
      {
        id: 1,
        username: 'alice',
        email: 'alice@example.com',
        role: 'USER',
      },
      {
        id: 2,
        username: 'bob-admin',
        email: 'bob@example.com',
        role: 'ADMIN',
      },
      {
        id: 3,
        username: 'carol',
        email: 'carol@example.com',
        role: 'USER',
      },
    ];

    usersService = jasmine.createSpyObj<AdminUsersService>('AdminUsersService', ['searchUsers', "createNewUser", "patchUser", "deleteUser"]);

    usersService.searchUsers.and.callFake((filters) => {
      const query = filters?.query.trim().toLowerCase() ?? '';
      const role = filters?.role ?? 'ALL';

      return of(
        registeredUsers.filter((user) => {
          const matchesQuery =
            !query ||
            user.username.toLowerCase().includes(query) ||
            user.email.toLowerCase().includes(query);

          const matchesRole =
            role === 'ALL' || user.role === role;

          return matchesQuery && matchesRole;
        }),
      );
    });

    usersService.createNewUser.and.callFake((dto) => {
      const newUser = {
        id: registeredUsers.length + 1,
        ...dto,
      };

      registeredUsers.push(newUser)

      return of<AdminUser>(newUser)
    })

    usersService.patchUser.and.callFake((dto) => {
      const user = registeredUsers.find((user) => user.id == dto.id)!

      user.email = dto.email
      user.username = dto.username
      user.role = dto.role

      return of(user);
    })

    usersService.deleteUser.and.callFake((userId) => {
      const user = registeredUsers.find(user => user.id == userId)!

      registeredUsers = registeredUsers.filter(user => user.id != userId);

      return of(user)
    })

    await TestBed.configureTestingModule({
      imports: [AdminUsersPageComponent],
      providers: [{provide: AdminUsersService, useValue: usersService}],
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminUsersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('displays all registered users in the admin panel', () => {
    expect(component).toBeTruthy();

    const rows = fixture.nativeElement.querySelectorAll('[data-testid="user-row"]');
    const pageText = fixture.nativeElement.textContent;

    expect(usersService.searchUsers).toHaveBeenCalledTimes(1);
    expect(rows.length).toBe(3);
    expect(pageText).toContain('alice@example.com');
    expect(pageText).toContain('bob@example.com');
    expect(pageText).toContain('carol@example.com');
    expect(pageText).toContain('ADMIN');
    expect(pageText).toContain('USER');
  });

  it('allows an admin to search users by name or email and filter by role', fakeAsync(() => {
    searchFor('alice');
    expect(renderedRows()).toEqual(['alice|alice@example.com|USER']);

    searchFor('bob@example.com');
    expect(renderedRows()).toEqual(['bob-admin|bob@example.com|ADMIN']);

    searchFor('');
    filterByRole('ADMIN');
    expect(renderedRows()).toEqual(['bob-admin|bob@example.com|ADMIN']);

    filterByRole('USER');
    expect(renderedRows()).toEqual([
      'alice|alice@example.com|USER',
      'carol|carol@example.com|USER',
    ]);

    filterByRole('ALL');
    expect(renderedRows()).toEqual([
      'alice|alice@example.com|USER',
      'bob-admin|bob@example.com|ADMIN',
      'carol|carol@example.com|USER',
    ]);
  }));

  it('allows an admin to create user by submitting form with account details', fakeAsync(() => {
    const modalTogglerElement = getElement<HTMLButtonElement>("add-user-modal-button")
    modalTogglerElement.click()

    const nameInput = getElement<HTMLInputElement>("user-form--username-input")
    const emailInput = getElement<HTMLInputElement>("user-form--email-input")
    const passwordInput = getElement<HTMLInputElement>("user-form--password-input")
    const roleSelect = getElement<HTMLSelectElement>("user-form--role-select")

    nameInput.value = "new_username";
    emailInput.value = "new_user@cieszczyk.pl";
    passwordInput.value = "passwd";
    roleSelect.value = "ADMIN"

    nameInput.dispatchEvent(new Event('input'))
    emailInput.dispatchEvent(new Event('input'))
    passwordInput.dispatchEvent(new Event('input'))
    roleSelect.dispatchEvent(new Event('change'))

    fixture.detectChanges();

    const submit = getElement<HTMLButtonElement>("user-form--submit")
    submit.click()

    fixture.detectChanges()
    tick()

    expect(usersService.createNewUser).toHaveBeenCalledTimes(1)

    expect(renderedRows()).toEqual([
      'alice|alice@example.com|USER',
      'bob-admin|bob@example.com|ADMIN',
      'carol|carol@example.com|USER',
      'new_username|new_user@cieszczyk.pl|ADMIN',
    ]);
  }));

  it('allows an admin to edit user by submitting form with account details', fakeAsync(() => {
    const modalTogglerElement = getElement<HTMLButtonElement>("edit-user-button")
    modalTogglerElement.click()

    const nameInput = getElement<HTMLInputElement>("user-form--username-input")
    const emailInput = getElement<HTMLInputElement>("user-form--email-input")
    const passwordInput = getElement<HTMLInputElement>("user-form--password-input")
    const roleSelect = getElement<HTMLSelectElement>("user-form--role-select")

    nameInput.value = "new_username";
    emailInput.value = "new_user@cieszczyk.pl";
    passwordInput.value = "passwd";
    roleSelect.value = "ADMIN"

    nameInput.dispatchEvent(new Event('input'))
    emailInput.dispatchEvent(new Event('input'))
    passwordInput.dispatchEvent(new Event('input'))
    roleSelect.dispatchEvent(new Event('change'))
    fixture.detectChanges();

    const submit = getElement<HTMLButtonElement>("user-form--submit")
    submit.click()

    fixture.detectChanges()
    tick()

    expect(usersService.patchUser).toHaveBeenCalledTimes(1)

    expect(renderedRows().sort()).toEqual([
      'new_username|new_user@cieszczyk.pl|ADMIN',
      'bob-admin|bob@example.com|ADMIN',
      'carol|carol@example.com|USER',
    ].sort());
  }))

  it('allows user to delete user', fakeAsync(() => {
    const userId = 1;
    const deleteButton = getElement<HTMLButtonElement>(`delete-user-button-${userId}`);
    deleteButton.click()

    const deleteConfirmationButton = getElement<HTMLButtonElement>(`delete-user--confirm`)
    deleteConfirmationButton.click();

    fixture.detectChanges()
    tick()

    expect(usersService.deleteUser).toHaveBeenCalledTimes(1)

    expect(renderedRows().sort()).toEqual([
      'bob-admin|bob@example.com|ADMIN',
      'carol|carol@example.com|USER',
    ].sort());
  }));

  function searchFor(query: string): void {
    const searchInput = getElement<HTMLInputElement>("user-search")

    searchInput.value = query;
    searchInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submitFilters();
  }

  function filterByRole(role: 'ALL' | 'ADMIN' | 'USER'): void {
    const roleFilter = getElement<HTMLSelectElement>("role-filter")

    roleFilter.value = role;
    roleFilter.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    submitFilters();
  }

  function submitFilters(): void {
    const submitButton = getElement<HTMLButtonElement>("search-submit")

    submitButton.click();
    fixture.detectChanges();
    tick();
  }

  function renderedRows(): string[] {
    return Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid="user-row"]') as NodeListOf<HTMLElement>,
    ).map((row) =>
      Array.from(row.querySelectorAll('td'))
        .filter(td => !td.querySelector("button"))
        .map((cell) => cell.textContent?.replace(/\s+/g, ' ').trim() ?? '')
        .join('|'),
    );
  }

  function getElement<T>(dataTestId: string) {
    const element = fixture.nativeElement.querySelector(`[data-testid="${dataTestId}"]`) as T

    if (!element)
      throw new Error(`Expected element with [data-testid="${dataTestId}"]`)

    return element;
  }
});
