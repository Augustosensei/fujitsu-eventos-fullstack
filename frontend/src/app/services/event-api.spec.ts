import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EventRequest, EventResponse } from '../models/event';
import { PageResponse } from '../models/page';
import { EventApi } from './event-api';
import { CONNECTION_ERROR_MESSAGE } from './ui-error-mapper';

const REQUEST: EventRequest = {
  eventId: 'EVT-1001',
  eventType: 'QUALITY',
  description: 'Quality check started',
  timestamp: '2026-05-20T10:00:00',
};

const RESPONSE: EventResponse = {
  ...REQUEST,
  timestamp: '2026-05-20T10:00:00',
  receivedAt: '2026-05-20T13:00:00.789Z',
};

const PAGE: PageResponse<EventResponse> = {
  content: [RESPONSE],
  page: 1,
  size: 5,
  totalElements: 6,
  totalPages: 2,
};

describe('EventApi', () => {
  let api: EventApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(EventApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('UI-12 given a request when register is called then it POSTs the body as-is to /api/events and resolves the created event', async () => {
    const result = api.register(REQUEST);

    const req = httpMock.expectOne('/api/events');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(REQUEST);
    req.flush(RESPONSE, { status: 201, statusText: 'Created' });

    await expect(result).resolves.toEqual({ ok: true, event: RESPONSE });
  });

  it('UI-03 given the backend answers 409 when register is called then it resolves the duplicate error with the backend detail', async () => {
    const detail =
      "El evento con eventId 'EVT-1001' ya estaba registrado; no se guardó ni se auditó de nuevo.";
    const result = api.register(REQUEST);

    httpMock
      .expectOne('/api/events')
      .flush(
        { detail, code: 'DUPLICATE_EVENT', status: 409 },
        { status: 409, statusText: 'Conflict' },
      );

    await expect(result).resolves.toEqual({
      ok: false,
      error: { code: 'DUPLICATE_EVENT', message: detail, fieldErrors: {} },
    });
  });

  it('UI-05 given the server is unreachable when register is called then it resolves the connection error', async () => {
    const result = api.register(REQUEST);

    httpMock.expectOne('/api/events').error(new ProgressEvent('error'));

    await expect(result).resolves.toEqual({
      ok: false,
      error: { code: null, message: CONNECTION_ERROR_MESSAGE, fieldErrors: {} },
    });
  });

  it('UI-12 given page and size when the events page resource loads then it GETs /api/events with that query and exposes the page', async () => {
    const resource = TestBed.runInInjectionContext(() => api.page(signal({ page: 1, size: 5 })));
    TestBed.tick();

    const req = httpMock.expectOne((r) => r.url === '/api/events');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('5');
    req.flush(PAGE);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(resource.value()).toEqual(PAGE);
  });

  it('UI-12 given the query signal changes when the resource reloads then it requests the new page', async () => {
    const query = signal({ page: 0, size: 5 });
    const resource = TestBed.runInInjectionContext(() => api.page(query));
    TestBed.tick();
    httpMock
      .expectOne((r) => r.url === '/api/events' && r.params.get('page') === '0')
      .flush({ ...PAGE, page: 0 });
    await TestBed.inject(ApplicationRef).whenStable();

    query.set({ page: 1, size: 5 });
    TestBed.tick();
    httpMock.expectOne((r) => r.url === '/api/events' && r.params.get('page') === '1').flush(PAGE);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(resource.value()).toEqual(PAGE);
  });
});
