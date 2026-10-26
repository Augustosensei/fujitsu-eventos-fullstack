import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuditResponse } from '../models/audit';
import { PageResponse } from '../models/page';
import { AuditApi } from './audit-api';
import { toUiError } from './ui-error-mapper';

const PAGE: PageResponse<AuditResponse> = {
  content: [{ id: 1, eventId: 'EVT-1001', auditedAt: '2026-05-20T13:00:00.789Z' }],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

describe('AuditApi', () => {
  let api: AuditApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AuditApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('UI-12 given page and size when the audits page resource loads then it GETs /api/audits with that query and exposes the page', async () => {
    const resource = TestBed.runInInjectionContext(() => api.page(signal({ page: 2, size: 7 })));
    TestBed.tick();

    const req = httpMock.expectOne((r) => r.url === '/api/audits');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('7');
    req.flush(PAGE);
    await TestBed.inject(ApplicationRef).whenStable();

    expect(resource.value()).toEqual(PAGE);
  });

  it('UI-12 given the backend rejects the pagination when the resource loads then its error maps to the backend detail', async () => {
    const resource = TestBed.runInInjectionContext(() => api.page(signal({ page: -1, size: 20 })));
    TestBed.tick();

    httpMock
      .expectOne((r) => r.url === '/api/audits')
      .flush(
        {
          detail: 'Los parámetros de paginación no son válidos.',
          code: 'INVALID_PAGINATION',
          errors: [{ field: 'page', message: 'El número de página debe ser 0 o mayor.' }],
        },
        { status: 400, statusText: 'Bad Request' },
      );
    await TestBed.inject(ApplicationRef).whenStable();

    expect(resource.hasValue()).toBe(false);
    expect(toUiError(resource.error())).toEqual({
      code: 'INVALID_PAGINATION',
      message: 'Los parámetros de paginación no son válidos.',
      fieldErrors: { page: ['El número de página debe ser 0 o mayor.'] },
    });
  });
});
