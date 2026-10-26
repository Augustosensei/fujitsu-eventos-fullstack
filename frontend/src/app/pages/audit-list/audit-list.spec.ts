import { formatDate } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuditResponse } from '../../models/audit';
import { PageResponse } from '../../models/page';
import { UNEXPECTED_ERROR_MESSAGE } from '../../services/ui-error-mapper';
import { AuditList } from './audit-list';

const AUDIT: AuditResponse = { id: 7, eventId: 'EVT-1002', auditedAt: '2026-05-20T13:00:00.789Z' };

function page(pageNumber: number, totalPages: number): PageResponse<AuditResponse> {
  return {
    content: [AUDIT],
    page: pageNumber,
    size: 20,
    totalElements: totalPages * 20,
    totalPages,
  };
}

describe('AuditList', () => {
  let fixture: ComponentFixture<AuditList>;
  let httpMock: HttpTestingController;
  let host: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'events/new', children: [] }]),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AuditList);
    host = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function text(): string {
    return host.textContent!.replace(/\s+/g, ' ');
  }

  async function respond(pageNumber: number, body: object, status = 200): Promise<void> {
    httpMock
      .expectOne((r) => r.url === '/api/audits' && r.params.get('page') === String(pageNumber))
      .flush(body, { status, statusText: String(status) });
    await fixture.whenStable();
  }

  it('UI-10 given the request is in flight when the page renders then a loading message is shown', async () => {
    expect(text()).toContain('Cargando auditorías…');

    await respond(0, page(0, 1));
  });

  it('UI-10 given audits when they load then the table shows the number, the eventId and the formatted date', async () => {
    await respond(0, page(0, 1));

    const cells = [...host.querySelectorAll('tbody td')].map((td) => td.textContent!.trim());
    expect(cells).toEqual([
      '7',
      'EVT-1002',
      formatDate(AUDIT.auditedAt, 'dd/MM/yyyy HH:mm:ss', 'en-US'),
    ]);
    expect(
      [...host.querySelectorAll('th[scope="col"]')].map((th) => th.textContent!.trim()),
    ).toEqual(['N.º de auditoría', 'Identificador del evento', 'Fecha de auditoría']);
  });

  it('UI-10 given the last page when it is shown then "Siguiente" is disabled and "Anterior" goes back', async () => {
    await respond(0, page(0, 2));
    const [previous, next] = host.querySelectorAll<HTMLButtonElement>('app-paginator button');
    expect(previous.disabled).toBe(true);

    next.click();
    fixture.detectChanges();
    await respond(1, page(1, 2));
    const [previousOnLast, nextOnLast] =
      host.querySelectorAll<HTMLButtonElement>('app-paginator button');
    expect(nextOnLast.disabled).toBe(true);

    previousOnLast.click();
    fixture.detectChanges();
    await respond(0, page(0, 2));
    expect(text()).toContain('Página 1 de 2');
  });

  it('UI-10 given a 500 when the page loads then the generic error is shown with a retry button', async () => {
    await respond(0, { detail: 'interno', code: 'INTERNAL_ERROR' }, 500);

    expect(text()).toContain(UNEXPECTED_ERROR_MESSAGE);
    host.querySelector<HTMLButtonElement>('app-status-message button')!.click();
    fixture.detectChanges();
    await respond(0, page(0, 1));

    expect(text()).toContain('EVT-1002');
  });

  it('UI-17 given no audits when the page loads then it explains how to create one and links to /events/new', async () => {
    await respond(0, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(text()).toContain('Todavía no hay auditorías.');
    host.querySelector<HTMLAnchorElement>('a.button')!.click();
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/events/new');
  });
});
