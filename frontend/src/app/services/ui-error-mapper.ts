import { HttpErrorResponse } from '@angular/common/http';
import { ERROR_CODES, ErrorCode, FieldError } from '../models/problem-detail';
import { UiError } from '../models/ui-error';

export const CONNECTION_ERROR_MESSAGE = 'No se pudo conectar con el servidor.';

export const UNEXPECTED_ERROR_MESSAGE = 'Ocurrió un error inesperado. Intentá de nuevo más tarde.';

const CONNECTION_ERROR: UiError = {
  code: null,
  message: CONNECTION_ERROR_MESSAGE,
  fieldErrors: {},
};

const UNEXPECTED_ERROR: UiError = {
  code: null,
  message: UNEXPECTED_ERROR_MESSAGE,
  fieldErrors: {},
};

export function toUiError(error: unknown): UiError {
  if (!(error instanceof HttpErrorResponse) || error.status >= 500) {
    return UNEXPECTED_ERROR;
  }
  if (error.status === 0) {
    return CONNECTION_ERROR;
  }
  const body: unknown = error.error;
  if (!isObject(body) || typeof body['detail'] !== 'string') {
    return UNEXPECTED_ERROR;
  }
  return {
    code: toErrorCode(body['code']),
    message: body['detail'],
    fieldErrors: groupByField(body['errors']),
  };
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function toErrorCode(value: unknown): ErrorCode | null {
  return ERROR_CODES.find((code) => code === value) ?? null;
}

function isFieldError(value: unknown): value is FieldError {
  return (
    isObject(value) && typeof value['field'] === 'string' && typeof value['message'] === 'string'
  );
}

function groupByField(errors: unknown): Record<string, readonly string[]> {
  if (!Array.isArray(errors)) {
    return {};
  }
  return errors.filter(isFieldError).reduce<Record<string, string[]>>((grouped, error) => {
    (grouped[error.field] ??= []).push(error.message);
    return grouped;
  }, {});
}
