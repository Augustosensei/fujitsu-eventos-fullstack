import { EventType } from './event-type';
import { UiError } from './ui-error';

export interface EventRequest {
  readonly eventId: string;
  readonly eventType: EventType;
  readonly description: string;
  readonly timestamp?: string;
}

export interface EventResponse {
  readonly eventId: string;
  readonly eventType: EventType;
  readonly description: string;
  readonly timestamp: string;
  readonly receivedAt: string;
}

export type RegisterResult =
  | { readonly ok: true; readonly event: EventResponse }
  | { readonly ok: false; readonly error: UiError };
