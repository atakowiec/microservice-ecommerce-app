import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ConfirmationModalComponent } from './confirmation-modal.component';

@Component({
  imports: [ConfirmationModalComponent],
  template: `
    <app-confirmation-modal
      title="Delete user?"
      message="This action cannot be undone."
      confirmLabel="Delete user"
      intent="danger"
      testIdPrefix="delete-user"
      [(open)]="open"
      [pending]="pending"
      (confirmed)="confirmCount = confirmCount + 1"
      (cancelled)="cancelCount = cancelCount + 1"
    />
  `,
})
class ConfirmationModalTestHostComponent {
  open = true;
  pending = false;
  confirmCount = 0;
  cancelCount = 0;
}

describe('ConfirmationModalComponent', () => {
  let fixture: ComponentFixture<ConfirmationModalTestHostComponent>;
  let host: ConfirmationModalTestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmationModalTestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmationModalTestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('uses the configured test id prefix for the modal and buttons', () => {
    expect(query('delete-user--modal')).toBeTruthy();
    expect(query('delete-user--close')).toBeTruthy();
    expect(query('delete-user--cancel')).toBeTruthy();
    expect(query('delete-user--confirm')).toBeTruthy();
  });

  it('emits confirmation without closing so the parent can await an async action', () => {
    click('delete-user--confirm');

    expect(host.confirmCount).toBe(1);
    expect(host.open).toBeTrue();
  });

  it('emits cancellation and closes from the cancel button', () => {
    click('delete-user--cancel');
    fixture.detectChanges();

    expect(host.cancelCount).toBe(1);
    expect(host.open).toBeFalse();
  });

  it('disables actions and shows the pending label while pending', () => {
    host.pending = true;
    fixture.detectChanges();

    const confirm = query('delete-user--confirm').nativeElement as HTMLButtonElement;
    const cancel = query('delete-user--cancel').nativeElement as HTMLButtonElement;

    expect(confirm.disabled).toBeTrue();
    expect(confirm.textContent).toContain('Working…');
    expect(cancel.disabled).toBeTrue();
  });

  function query(testId: string) {
    return fixture.debugElement.query(By.css(`[data-testid="${testId}"]`));
  }

  function click(testId: string): void {
    (query(testId).nativeElement as HTMLButtonElement).click();
    fixture.detectChanges();
  }
});
