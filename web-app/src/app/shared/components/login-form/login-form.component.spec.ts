import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { LoginFormComponent } from './login-form.component';

describe('LoginFormComponent', () => {
  let fixture: ComponentFixture<LoginFormComponent>;
  let component: LoginFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginFormComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('eyebrow', 'Admin portal');
    fixture.componentRef.setInput('heading', 'Log in');
    fixture.componentRef.setInput('description', 'Enter your credentials.');
    fixture.componentRef.setInput('submitLabel', 'Log in');
    fixture.detectChanges();
  });

  it('displays username and password fields', () => {
    const username = fixture.debugElement.query(By.css('#username')).nativeElement as HTMLInputElement;
    const password = fixture.debugElement.query(By.css('#password')).nativeElement as HTMLInputElement;

    expect(username).toBeTruthy();
    expect(password).toBeTruthy();
    expect(password.type).toBe('password');
  });

  it('validates both required fields before submission', () => {
    const emitSpy = spyOn(component.loginSubmitted, 'emit');

    clickSubmit();

    expect(emitSpy).not.toHaveBeenCalled();
    expect(component.form.controls.username.hasError('required')).toBeTrue();
    expect(component.form.controls.password.hasError('required')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Username is required.');
    expect(fixture.nativeElement.textContent).toContain('Password is required.');
  });

  it('emits credentials only when both fields are valid', () => {
    const emitSpy = spyOn(component.loginSubmitted, 'emit');
    enterCredentials('admin@example.com', 'correct-password');

    clickSubmit();

    expect(emitSpy).toHaveBeenCalledOnceWith({
      username: 'admin@example.com',
      password: 'correct-password',
    });
  });

  it('masks the password by default and toggles its visibility', () => {
    const password = fixture.debugElement.query(By.css('#password')).nativeElement as HTMLInputElement;
    const toggle = fixture.debugElement.query(By.css('.password-toggle')).nativeElement as HTMLButtonElement;

    expect(password.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Show password');

    toggle.click();
    fixture.detectChanges();

    expect(password.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Hide password');

    toggle.click();
    fixture.detectChanges();

    expect(password.type).toBe('password');
  });

  function enterCredentials(username: string, password: string): void {
    const usernameInput = fixture.debugElement.query(By.css('#username')).nativeElement as HTMLInputElement;
    const passwordInput = fixture.debugElement.query(By.css('#password')).nativeElement as HTMLInputElement;

    usernameInput.value = username;
    usernameInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function clickSubmit(): void {
    const submit = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();
  }
});
