import { Component, input, model, output } from '@angular/core';

import {
  ModalCloseReason,
  ModalComponent,
  ModalSize,
} from '../modal/modal.component';

export type ConfirmationModalIntent = 'default' | 'danger';

@Component({
  selector: 'app-confirmation-modal',
  imports: [ModalComponent],
  templateUrl: './confirmation-modal.component.html',
  styleUrl: './confirmation-modal.component.scss',
})
export class ConfirmationModalComponent {
  readonly open = model(false);
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input('Confirm');
  readonly cancelLabel = input('Cancel');
  readonly pendingLabel = input('Working…');
  readonly pending = input(false);
  readonly intent = input<ConfirmationModalIntent>('default');
  readonly size = input<ModalSize>('small');
  readonly testIdPrefix = input('confirmation');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
  readonly closed = output<ModalCloseReason>();

  confirm(): void {
    if (!this.pending()) {
      this.confirmed.emit();
    }
  }

  cancel(): void {
    if (this.pending()) {
      return;
    }

    this.cancelled.emit();
    this.open.set(false);
  }

  handleModalClosed(reason: ModalCloseReason): void {
    this.closed.emit(reason);

    if (reason !== 'programmatic') {
      this.cancelled.emit();
    }
  }

  testId(part: 'cancel' | 'confirm'): string {
    return `${this.testIdPrefix().trim()}--${part}`;
  }
}
