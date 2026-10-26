export const ERROR_CODES = [
  'VALIDATION_ERROR',
  'MALFORMED_REQUEST',
  'INVALID_PAGINATION',
  'DUPLICATE_EVENT',
  'EVENT_NOT_FOUND',
  'RESOURCE_NOT_FOUND',
  'METHOD_NOT_ALLOWED',
  'UNSUPPORTED_MEDIA_TYPE',
  'NOT_ACCEPTABLE',
  'INTERNAL_ERROR',
] as const;

export type ErrorCode = (typeof ERROR_CODES)[number];

export interface FieldError {
  readonly field: string;
  readonly message: string;
}

export interface ProblemDetail {
  readonly type?: string;
  readonly title: string;
  readonly status: number;
  readonly detail: string;
  readonly instance?: string;
  readonly code: ErrorCode;
  readonly errors?: readonly FieldError[];
  readonly correlationId: string;
}
