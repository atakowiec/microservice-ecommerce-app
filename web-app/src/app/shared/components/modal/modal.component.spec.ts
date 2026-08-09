import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ModalCloseReason, ModalComponent } from './modal.component';

@Component({
  imports: [ModalComponent],
  template: `
    <button type="button" data-testid="open-modal" (click)="modalOpen = true">Open</button>

    <app-modal
      title="Example modal"
      description="Reusable modal content"
      [(open)]="modalOpen"
      (closed)="lastCloseReason = $event"
    >
      <p data-testid="projected-body">Body content</p>
      <button modal-footer type="button">Confirm</button>
    </app-modal>
  `,
})
class ModalTestHostComponent {
  modalOpen = false;
  lastCloseReason: ModalCloseReason | null = null;
}

describe('ModalComponent', () => {
  let fixture: ComponentFixture<ModalTestHostComponent>;
  let host: ModalTestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalTestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ModalTestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('opens as a modal and renders projected body and footer content', () => {
    openModal();

    const dialog = getDialog();
    expect(dialog.open).toBeTrue();
    expect(dialog.getAttribute('aria-labelledby')).toBeTruthy();
    expect(fixture.debugElement.query(By.css('[data-testid="projected-body"]'))).toBeTruthy();
    expect(dialog.textContent).toContain('Confirm');
  });

  it('closes from the close button and updates two-way open state', () => {
    openModal();

    const closeButton = fixture.debugElement.query(
      By.css('[data-testid="modal-close"]'),
    ).nativeElement as HTMLButtonElement;
    closeButton.click();
    fixture.detectChanges();

    expect(getDialog().open).toBeFalse();
    expect(host.modalOpen).toBeFalse();
    expect(host.lastCloseReason).toBe('close-button');
  });

  it('closes on Escape and reports the close reason', () => {
    openModal();
    const cancelEvent = new Event('cancel', { cancelable: true });

    getDialog().dispatchEvent(cancelEvent);
    fixture.detectChanges();

    expect(cancelEvent.defaultPrevented).toBeTrue();
    expect(host.modalOpen).toBeFalse();
    expect(host.lastCloseReason).toBe('escape');
  });

  it('closes when the backdrop is clicked', () => {
    openModal();

    getDialog().dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(host.modalOpen).toBeFalse();
    expect(host.lastCloseReason).toBe('backdrop');
  });

  function openModal(): void {
    const openButton = fixture.debugElement.query(
      By.css('[data-testid="open-modal"]'),
    ).nativeElement as HTMLButtonElement;
    openButton.click();
    fixture.detectChanges();
  }

  function getDialog(): HTMLDialogElement {
    return fixture.debugElement.query(By.css('dialog')).nativeElement as HTMLDialogElement;
  }
});
