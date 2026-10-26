import { HttpErrorResponse } from '@angular/common/http';
import { CONNECTION_ERROR_MESSAGE, UNEXPECTED_ERROR_MESSAGE, toUiError } from './ui-error-mapper';

function httpError(status: number, body: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: body, url: '/api/events' });
}

const UNEXPECTED = { code: null, message: UNEXPECTED_ERROR_MESSAGE, fieldErrors: {} };

describe('toUiError', () => {
  it('UI-03 given a 409 DUPLICATE_EVENT problem when mapped then the message is the backend detail', () => {
    const detail =
      "El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo.";

    const error = toUiError(
      httpError(409, {
        title: 'Evento duplicado',
        status: 409,
        detail,
        code: 'DUPLICATE_EVENT',
        correlationId: 'abc',
      }),
    );

    expect(error).toEqual({ code: 'DUPLICATE_EVENT', message: detail, fieldErrors: {} });
  });

  it('UI-04 given a 400 problem with errors when mapped then each message is grouped under its field in backend order', () => {
    const detail = 'La solicitud tiene datos inválidos. Revisá los campos indicados.';

    const error = toUiError(
      httpError(400, {
        detail,
        code: 'VALIDATION_ERROR',
        errors: [
          { field: 'description', message: 'La descripción es obligatoria.' },
          { field: 'eventId', message: 'El eventId puede tener como máximo 64 caracteres.' },
          {
            field: 'eventId',
            message:
              'El eventId solo admite letras sin acentos, números, guion (-) y guion bajo (_), sin espacios.',
          },
        ],
      }),
    );

    expect(error).toEqual({
      code: 'VALIDATION_ERROR',
      message: detail,
      fieldErrors: {
        description: ['La descripción es obligatoria.'],
        eventId: [
          'El eventId puede tener como máximo 64 caracteres.',
          'El eventId solo admite letras sin acentos, números, guion (-) y guion bajo (_), sin espacios.',
        ],
      },
    });
  });

  it('UI-04 given a 400 problem without errors when mapped then there are no field errors', () => {
    const detail =
      'El cuerpo de la solicitud falta o no es un JSON válido con un objeto como raíz.';

    const error = toUiError(httpError(400, { detail, code: 'MALFORMED_REQUEST' }));

    expect(error).toEqual({ code: 'MALFORMED_REQUEST', message: detail, fieldErrors: {} });
  });

  it('UI-05 given a network failure (status 0) when mapped then it says the server could not be reached', () => {
    const error = toUiError(
      new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }),
    );

    expect(error).toEqual({ code: null, message: CONNECTION_ERROR_MESSAGE, fieldErrors: {} });
    expect(CONNECTION_ERROR_MESSAGE).toBe('No se pudo conectar con el servidor.');
  });

  it('UI-05 given a 500 problem when mapped then it shows the fixed generic message instead of the body', () => {
    const error = toUiError(
      httpError(500, { detail: 'detalle del servidor', code: 'INTERNAL_ERROR' }),
    );

    expect(error).toEqual(UNEXPECTED);
    expect(UNEXPECTED_ERROR_MESSAGE).toBe(
      'Ocurrió un error inesperado. Intentá de nuevo más tarde.',
    );
  });

  it.each([502, 503, 504])(
    'UI-05 given a %i from a proxy when mapped then it shows the generic message',
    (status) => {
      expect(toUiError(httpError(status, '<html>Bad Gateway</html>'))).toEqual(UNEXPECTED);
    },
  );

  it('UI-05 given a 4xx whose body is not a problem detail when mapped then it shows the generic message', () => {
    expect(toUiError(httpError(404, '<html>Not Found</html>'))).toEqual(UNEXPECTED);
    expect(toUiError(httpError(400, null))).toEqual(UNEXPECTED);
    expect(toUiError(httpError(400, { title: 'Bad Request', detail: 42 }))).toEqual(UNEXPECTED);
  });

  it('UI-05 given an error that is not an HTTP response when mapped then it shows the generic message', () => {
    expect(toUiError(new TypeError('boom'))).toEqual(UNEXPECTED);
  });

  it('UI-12 given a problem with an unknown code and malformed errors when mapped then it keeps the detail and only well-formed field errors', () => {
    const error = toUiError(
      httpError(400, {
        detail: 'Datos inválidos.',
        code: 'SOMETHING_ELSE',
        errors: [
          { field: 'eventId' },
          'texto',
          null,
          { field: 'page', message: 'El número de página debe ser 0 o mayor.' },
        ],
      }),
    );

    expect(error).toEqual({
      code: null,
      message: 'Datos inválidos.',
      fieldErrors: { page: ['El número de página debe ser 0 o mayor.'] },
    });
  });

  it('UI-12 given a problem whose errors is not a list when mapped then there are no field errors', () => {
    const error = toUiError(
      httpError(400, { detail: 'Datos inválidos.', code: 'VALIDATION_ERROR', errors: 'eventId' }),
    );

    expect(error.fieldErrors).toEqual({});
  });
});
