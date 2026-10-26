import { Component, input } from '@angular/core';

export type StatusKind = 'loading' | 'info' | 'success' | 'error';

@Component({
  selector: 'app-status-message',
  styleUrl: './status-message.css',
  templateUrl: './status-message.html',
  host: {
    '[class]': "'message message--' + kind()",
    '[attr.role]': "kind() === 'error' ? 'alert' : 'status'",
    '[attr.aria-live]': "kind() === 'error' ? 'assertive' : 'polite'",
  },
})
export class StatusMessage {
  readonly kind = input.required<StatusKind>();
}
