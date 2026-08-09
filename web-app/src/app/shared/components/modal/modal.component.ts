import { Component, ElementRef, effect, input, model, output, viewChild } from '@angular/core';

export type ModalSize = 'small' | 'medium' | 'large';
export type ModalCloseReason = 'close-button' | 'backdrop' | 'escape' | 'programmatic';

let nextModalId = 0;

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  private readonly dialogElement = viewChild<ElementRef<HTMLDialogElement>>('dialog');
  private ignoreNextNativeClose = false;

  readonly open = model(false);
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly size = input<ModalSize>('medium');
  readonly closeOnBackdrop = input(true);
  readonly closeOnEscape = input(true);
  readonly testIdPrefix = input<string | null>(null);
  readonly closed = output<ModalCloseReason>();

  readonly titleId = `app-modal-title-${nextModalId++}`;
  readonly descriptionId = `app-modal-description-${nextModalId++}`;

  constructor() {
    effect(() => {
      const dialog = this.dialogElement()?.nativeElement;
      const shouldBeOpen = this.open();

      if (!dialog) {
        return;
      }

      if (shouldBeOpen && !dialog.open) {
        dialog.showModal();
      } else if (!shouldBeOpen && dialog.open) {
        this.ignoreNextNativeClose = true;
        dialog.close();
        this.closed.emit('programmatic');
      }
    });
  }

  show(): void {
    this.open.set(true);
  }

  testId(part: 'modal' | 'close'): string {
    const prefix = this.testIdPrefix()?.trim();

    if (prefix) {
      return `${prefix}--${part}`;
    }

    return part === 'modal' ? 'modal' : 'modal-close';
  }

  close(reason: ModalCloseReason = 'programmatic'): void {
    const dialog = this.dialogElement()?.nativeElement;

    if (!this.open() && !dialog?.open) {
      return;
    }

    this.open.set(false);
    this.closed.emit(reason);

    if (dialog?.open) {
      this.ignoreNextNativeClose = true;
      dialog.close();
    }
  }

  onCancel(event: Event): void {
    event.preventDefault();

    if (this.closeOnEscape()) {
      this.close('escape');
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && this.closeOnBackdrop()) {
      this.close('backdrop');
    }
  }

  onNativeClose(): void {
    if (this.ignoreNextNativeClose) {
      this.ignoreNextNativeClose = false;
      return;
    }

    if (this.open()) {
      this.open.set(false);
    }

    this.closed.emit('programmatic');
  }
}
