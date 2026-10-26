import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  FieldTree,
  FormField,
  FormRoot,
  ReadonlyFieldState,
  ValidationError,
  form,
  maxLength,
  maxLengthError,
  patternError,
  required,
  requiredError,
  validate,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { EventRequest, RegisterResult } from '../../models/event';
import {
  DESCRIPTION_MAX_LENGTH,
  EVENT_ID_MAX_LENGTH,
  EVENT_ID_PATTERN,
  isBlank,
  strip,
} from '../../models/event-constraints';
import { EVENT_TYPE_OPTIONS, EventType } from '../../models/event-type';
import { UiError } from '../../models/ui-error';
import { EventApi } from '../../services/event-api';
import { FieldErrors } from '../shared/field-errors/field-errors';
import { StatusMessage } from '../shared/status-message/status-message';

interface RegisterModel {
  eventId: string;
  eventType: EventType | '';
  description: string;
  timestamp: string;
}

type FieldName = keyof RegisterModel;

const FIELD_ORDER: readonly FieldName[] = ['eventId', 'eventType', 'description', 'timestamp'];

export const REGISTER_MESSAGES = {
  eventIdRequired: 'El eventId es obligatorio.',
  eventIdTooLong: `El eventId puede tener como máximo ${EVENT_ID_MAX_LENGTH} caracteres.`,
  eventIdPattern:
    'El eventId solo admite letras sin acentos, números, guion (-) y guion bajo (_), sin espacios.',
  eventTypeRequired: 'El tipo de evento es obligatorio.',
  descriptionRequired: 'La descripción es obligatoria.',
  descriptionTooLong: `La descripción puede tener como máximo ${DESCRIPTION_MAX_LENGTH} caracteres.`,
} as const;

function eventIdErrors(rawEventId: string): ValidationError.WithoutFieldTree[] {
  const eventId = strip(rawEventId);
  if (eventId === '') {
    return [requiredError({ message: REGISTER_MESSAGES.eventIdRequired })];
  }
  const errors: ValidationError.WithoutFieldTree[] = [];
  if (eventId.length > EVENT_ID_MAX_LENGTH) {
    errors.push(maxLengthError(EVENT_ID_MAX_LENGTH, { message: REGISTER_MESSAGES.eventIdTooLong }));
  }
  if (!EVENT_ID_PATTERN.test(eventId)) {
    errors.push(patternError(EVENT_ID_PATTERN, { message: REGISTER_MESSAGES.eventIdPattern }));
  }
  return errors;
}

@Component({
  selector: 'app-event-register',
  imports: [FormField, FormRoot, RouterLink, FieldErrors, StatusMessage],
  styleUrl: './event-register.css',
  templateUrl: './event-register.html',
})
export class EventRegister {
  private readonly eventApi = inject(EventApi);
  private readonly injector = inject(Injector);
  private readonly resultMessage = viewChild.required('resultMessage', {
    read: ElementRef<HTMLElement>,
  });

  protected readonly eventTypeOptions = EVENT_TYPE_OPTIONS;
  protected readonly descriptionMaxLength = DESCRIPTION_MAX_LENGTH;
  protected readonly result = signal<RegisterResult | null>(null);

  private readonly model = signal<RegisterModel>({
    eventId: '',
    eventType: '',
    description: '',
    timestamp: '',
  });

  protected readonly registerForm = form(
    this.model,
    (path) => {
      validate(path.eventId, ({ value }) => eventIdErrors(value()));
      required(path.eventType, { message: REGISTER_MESSAGES.eventTypeRequired });
      validate(path.description, ({ value }) =>
        isBlank(value()) ? requiredError({ message: REGISTER_MESSAGES.descriptionRequired }) : [],
      );
      maxLength(path.description, DESCRIPTION_MAX_LENGTH, {
        message: REGISTER_MESSAGES.descriptionTooLong,
      });
    },
    {
      submission: {
        action: () => this.register(),
        onInvalid: () => this.focusFirstInvalid(),
      },
    },
  );

  protected showsErrors(state: ReadonlyFieldState<unknown>): boolean {
    return state.touched() && state.invalid();
  }

  private async register(): Promise<ValidationError.WithOptionalFieldTree[]> {
    this.result.set(null);
    const result = await this.eventApi.register(this.toRequest());
    this.result.set(result);
    afterNextRender(() => this.resultMessage().nativeElement.scrollIntoView({ block: 'nearest' }), {
      injector: this.injector,
    });
    return result.ok ? [] : this.toFieldErrors(result.error);
  }

  private toRequest(): EventRequest {
    const { eventId, eventType, description, timestamp } = this.model();
    return {
      eventId: strip(eventId),
      eventType: eventType as EventType,
      description,
      ...(timestamp === '' ? {} : { timestamp }),
    };
  }

  private toFieldErrors(error: UiError): ValidationError.WithOptionalFieldTree[] {
    return Object.entries(error.fieldErrors).flatMap(([field, messages]) => {
      const fieldTree = this.fieldTree(field);
      return fieldTree ? messages.map((message) => ({ kind: 'server', message, fieldTree })) : [];
    });
  }

  private fieldTree(name: string): FieldTree<unknown> | undefined {
    const fieldName = FIELD_ORDER.find((candidate) => candidate === name);
    return fieldName && this.registerForm[fieldName];
  }

  private focusFirstInvalid(): void {
    this.result.set(null);
    FIELD_ORDER.map((name) => this.registerForm[name])
      .find((field) => field().invalid())?.()
      .focusBoundControl();
  }
}
