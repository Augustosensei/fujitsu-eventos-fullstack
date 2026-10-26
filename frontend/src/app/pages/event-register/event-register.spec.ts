import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EventRequest, EventResponse } from '../../models/event';
import { CONNECTION_ERROR_MESSAGE, UNEXPECTED_ERROR_MESSAGE } from '../../services/ui-error-mapper';
import { EventRegister, REGISTER_MESSAGES } from './event-register';

type Control = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

const DUPLICATE_DETAIL =
  "El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo.";

describe('EventRegister', () => {
  let fixture: ComponentFixture<EventRegister>;
  let httpMock: HttpTestingController;
  let host: HTMLElement;
  let scrollIntoView: ReturnType<typeof vi.fn<HTMLElement['scrollIntoView']>>;

  beforeEach(async () => {
    scrollIntoView = vi.fn<HTMLElement['scrollIntoView']>();
    HTMLElement.prototype.scrollIntoView = scrollIntoView;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EventRegister);
    host = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
    delete (HTMLElement.prototype as Partial<HTMLElement>).scrollIntoView;
  });

  function control<T extends Control>(id: string): T {
    return host.querySelector<T>(`#${id}`)!;
  }

  function submitButton(): HTMLButtonElement {
    return host.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  function errorsOf(id: string): string[] {
    return [...host.querySelectorAll(`#${id}-errors .field-error`)].map((e) =>
      e.textContent!.trim(),
    );
  }

  function text(): string {
    return host.textContent!.replace(/\s+/g, ' ');
  }

  async function type(id: string, value: string): Promise<void> {
    const element = control(id);
    element.value = value;
    element.dispatchEvent(new Event('input'));
    element.dispatchEvent(new Event('change'));
    element.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function fill(values: Partial<Record<keyof EventRequest, string>>): Promise<void> {
    for (const [id, value] of Object.entries(values)) {
      await type(id, value);
    }
  }

  const VALID = { eventId: 'EVT-1001', eventType: 'QUALITY', description: 'Quality check' };

  function expectRegisterRequest(): ReturnType<HttpTestingController['expectOne']> {
    const req = httpMock.expectOne('/api/events');
    expect(req.request.method).toBe('POST');
    return req;
  }

  function created(request: EventRequest): EventResponse {
    return {
      ...request,
      timestamp: request.timestamp ?? '2026-05-20T10:00:00',
      receivedAt: '2026-05-20T13:00:00.789Z',
    };
  }

  async function submitAndFlush(body: object | null, status = 201): Promise<EventRequest> {
    submitButton().click();
    const req = expectRegisterRequest();
    const sent = req.request.body as EventRequest;
    req.flush(body ?? created(sent), { status, statusText: String(status) });
    await fixture.whenStable();
    return sent;
  }

  it('UI-01 given invalid values when the fields are touched then each one shows the backend message under it', async () => {
    await fill({ eventId: `${'A'.repeat(65)}!`, description: '   ' });

    expect(errorsOf('eventId')).toEqual([
      REGISTER_MESSAGES.eventIdTooLong,
      REGISTER_MESSAGES.eventIdPattern,
    ]);
    expect(errorsOf('description')).toEqual([REGISTER_MESSAGES.descriptionRequired]);
    expect(control('eventId').getAttribute('aria-invalid')).toBe('true');

    await type('description', 'x'.repeat(501));
    expect(errorsOf('description')).toEqual([REGISTER_MESSAGES.descriptionTooLong]);
  });

  it('UI-01 given the form is invalid when it renders then the button looks disabled with aria-disabled and enables once valid (DT29)', async () => {
    expect(submitButton().getAttribute('aria-disabled')).toBe('true');
    expect(submitButton().disabled).toBe(false);

    await fill(VALID);

    expect(submitButton().hasAttribute('aria-disabled')).toBe(false);
    expect(errorsOf('eventId')).toEqual([]);
  });

  it('UI-02 given a PRODUCTION event when it is registered then a success message without audit is shown', async () => {
    await fill({ ...VALID, eventType: 'PRODUCTION' });

    await submitAndFlush(null);

    const message = host.querySelector('app-status-message')!;
    expect(message.getAttribute('role')).toBe('status');
    expect(message.textContent).toContain('Evento EVT-1001 registrado correctamente.');
    expect(message.textContent).not.toContain('auditoría');
  });

  it('UI-02 given a QUALITY event when it is registered then the success message mentions the audit', async () => {
    await fill(VALID);

    await submitAndFlush(null);

    expect(text()).toContain('Evento EVT-1001 registrado correctamente.');
    expect(text()).toContain('Por ser de calidad, se generó su registro de auditoría.');
  });

  it('UI-03 given the backend answers 409 when submitting then the duplicate detail is shown as an alert', async () => {
    await fill(VALID);

    await submitAndFlush({ detail: DUPLICATE_DETAIL, code: 'DUPLICATE_EVENT' }, 409);

    const message = host.querySelector('app-status-message')!;
    expect(message.getAttribute('role')).toBe('alert');
    expect(message.textContent).toContain(DUPLICATE_DETAIL);
  });

  it('UI-02 given a success when the message is rendered then it is scrolled into view', async () => {
    await fill(VALID);

    await submitAndFlush(null);

    expect(scrollIntoView).toHaveBeenCalledOnce();
    expect(scrollIntoView.mock.contexts[0]).toBe(host.querySelector('app-status-message'));
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' });
  });

  it('UI-03 given a duplicate when the alert is rendered then it is scrolled into view', async () => {
    await fill(VALID);

    await submitAndFlush({ detail: DUPLICATE_DETAIL, code: 'DUPLICATE_EVENT' }, 409);

    expect(scrollIntoView).toHaveBeenCalledOnce();
    expect(scrollIntoView.mock.contexts[0]).toBe(host.querySelector('app-status-message'));
  });

  it('UI-04 given a 400 with errors when submitting then each message appears under its field and clears when edited', async () => {
    await fill(VALID);

    await submitAndFlush(
      {
        detail: 'La solicitud tiene datos inválidos. Revisá los campos indicados.',
        code: 'VALIDATION_ERROR',
        errors: [{ field: 'timestamp', message: "'timestamp' debe tener formato ISO-8601." }],
      },
      400,
    );

    expect(text()).toContain('La solicitud tiene datos inválidos. Revisá los campos indicados.');
    expect(errorsOf('timestamp')).toEqual(["'timestamp' debe tener formato ISO-8601."]);

    await type('timestamp', '2026-05-20T10:00:00');
    expect(errorsOf('timestamp')).toEqual([]);
  });

  it('UI-04 given a 400 error for a field the form does not have when submitting then only the general message is shown', async () => {
    await fill(VALID);

    await submitAndFlush(
      {
        detail: 'La solicitud tiene datos inválidos.',
        code: 'VALIDATION_ERROR',
        errors: [{ field: 'other', message: 'x' }],
      },
      400,
    );

    expect(text()).toContain('La solicitud tiene datos inválidos.');
    expect(host.querySelectorAll('.field-error').length).toBe(0);
  });

  it('UI-05 given the server cannot be reached when submitting then the connection message is shown', async () => {
    await fill(VALID);

    submitButton().click();
    expectRegisterRequest().error(new ProgressEvent('error'));
    await fixture.whenStable();

    expect(text()).toContain(CONNECTION_ERROR_MESSAGE);
  });

  it('UI-05 given a 500 when submitting then the generic message is shown', async () => {
    await fill(VALID);

    await submitAndFlush({ detail: 'interno', code: 'INTERNAL_ERROR' }, 500);

    expect(text()).toContain(UNEXPECTED_ERROR_MESSAGE);
    expect(text()).not.toContain('interno');
  });

  it('UI-06 given a successful registration when the same form is sent again then the values are kept and the 409 is visible', async () => {
    await fill(VALID);
    await submitAndFlush(null);

    expect(control('eventId').value).toBe('EVT-1001');
    expect(control('eventType').value).toBe('QUALITY');
    expect(control('description').value).toBe('Quality check');

    const resent = await submitAndFlush({ detail: DUPLICATE_DETAIL, code: 'DUPLICATE_EVENT' }, 409);

    expect(resent.eventId).toBe('EVT-1001');
    expect(text()).toContain(DUPLICATE_DETAIL);
  });

  it('UI-07 given a request in flight when the user submits again then the button is disabled and only one request is sent', async () => {
    await fill(VALID);

    submitButton().click();
    fixture.detectChanges();

    expect(submitButton().disabled).toBe(true);
    expect(submitButton().textContent).toContain('Registrando…');
    submitButton().click();
    host.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));

    const req = expectRegisterRequest();
    req.flush(created(req.request.body as EventRequest), { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    expect(submitButton().disabled).toBe(false);
  });

  it('UI-08 given an empty timestamp when submitting then it is omitted from the payload', async () => {
    await fill(VALID);

    const sent = await submitAndFlush(null);

    expect(sent).toEqual({
      eventId: 'EVT-1001',
      eventType: 'QUALITY',
      description: 'Quality check',
    });
    expect('timestamp' in sent).toBe(false);
  });

  it.each(['2026-05-20T10:00:05', '2026-05-20T10:00'])(
    'UI-08 given the timestamp %s when submitting then it is sent as typed',
    async (timestamp) => {
      await fill({ ...VALID, timestamp });

      const sent = await submitAndFlush(null);

      expect(sent.timestamp).toBe(timestamp);
    },
  );

  it('UI-08 given the timestamp field when it renders then it is a datetime-local input with seconds', () => {
    expect(control('timestamp').getAttribute('type')).toBe('datetime-local');
    expect(control('timestamp').getAttribute('step')).toBe('1');
  });

  it('UI-13 given the description when the user types then the counter shows n/500', async () => {
    const counter = () => host.querySelector('#description-counter')!.textContent!.trim();
    expect(counter()).toBe('0/500');

    await type('description', 'Hola');

    expect(counter()).toBe('4/500');
  });

  it('UI-14 given the form when it renders then the friendly options and the help texts are visible', () => {
    const select = control<HTMLSelectElement>('eventType');
    const options = [...select.options].map((o) => o.textContent!.trim());

    expect(options).toEqual([
      'Elegí un tipo de evento',
      'Producción (PRODUCTION)',
      'Calidad (QUALITY): genera auditoría',
    ]);
    expect(select.value).toBe('');
    expect(text()).toContain('Identificador del equipo de origen, p. ej. EVT-1001');
    expect(text()).toContain('Si lo dejás vacío, se usa la fecha y hora del servidor');
    expect(host.querySelector('.form__note .visually-hidden')!.textContent).toBe('asterisco');
  });

  it('UI-15 given an empty form when the user submits then nothing is sent, every required field shows its error and the first one gets focus', async () => {
    submitButton().click();
    await fixture.whenStable();

    httpMock.expectNone('/api/events');
    expect(errorsOf('eventId')).toEqual([REGISTER_MESSAGES.eventIdRequired]);
    expect(errorsOf('eventType')).toEqual([REGISTER_MESSAGES.eventTypeRequired]);
    expect(errorsOf('description')).toEqual([REGISTER_MESSAGES.descriptionRequired]);
    expect(errorsOf('timestamp')).toEqual([]);
    expect(document.activeElement).toBe(control('eventId'));
  });

  it('UI-15 given only the type is missing when the user submits then the focus goes to the type selector', async () => {
    await fill({ eventId: 'EVT-1', description: 'ok' });

    submitButton().click();
    await fixture.whenStable();

    httpMock.expectNone('/api/events');
    expect(document.activeElement).toBe(control('eventType'));
  });

  it('UI-16 given a QUALITY success when the message is shown then it links to the events and the audits', async () => {
    await fill(VALID);

    await submitAndFlush(null);

    const links = [...host.querySelectorAll('app-status-message a')].map((a) => [
      a.textContent!.trim(),
      a.getAttribute('href'),
    ]);
    expect(links).toEqual([
      ['Ver eventos registrados', '/events'],
      ['Ver auditorías', '/audits'],
    ]);
  });

  it('UI-16 given a PRODUCTION success or a duplicate when the message is shown then it offers only the events link or explains nothing was saved', async () => {
    await fill({ ...VALID, eventType: 'PRODUCTION' });
    await submitAndFlush(null);
    expect(
      [...host.querySelectorAll('app-status-message a')].map((a) => a.textContent!.trim()),
    ).toEqual(['Ver eventos registrados']);

    await submitAndFlush({ detail: DUPLICATE_DETAIL, code: 'DUPLICATE_EVENT' }, 409);
    expect(text()).toContain('no se guardó ni se auditó de nuevo');
  });

  it('UI-19 given an eventId with spaces and a tab around when submitting then it is valid and sent stripped', async () => {
    await fill({ ...VALID, eventId: '\t EVT-1001  ' });

    expect(errorsOf('eventId')).toEqual([]);
    const sent = await submitAndFlush(null);

    expect(sent.eventId).toBe('EVT-1001');
  });

  it('UI-19 given an eventId wrapped in a non-breaking space when validated then it is rejected like the backend strip does', async () => {
    await type('eventId', '\u00A0EVT-1001');

    expect(errorsOf('eventId')).toEqual([REGISTER_MESSAGES.eventIdPattern]);
  });
});
