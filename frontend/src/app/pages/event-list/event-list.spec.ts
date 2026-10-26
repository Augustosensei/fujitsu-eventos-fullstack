import { formatDate } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { EventResponse } from '../../models/event';
import { PageResponse } from '../../models/page';
import { CONNECTION_ERROR_MESSAGE } from '../../services/ui-error-mapper';
import { EventList } from './event-list';

const QUALITY_EVENT: EventResponse = {
  eventId: 'EVT-1002',
  eventType: 'QUALITY',
  description: 'Quality check started',
  timestamp: '2026-05-20T10:00:00',
  receivedAt: '2026-05-20T13:00:00.789Z',
};

const PRODUCTION_EVENT: EventResponse = {
  eventId: 'EVT-1001',
  eventType: 'PRODUCTION',
  description: 'Production line started',
  timestamp: '2026-05-20T09:30:15',
  receivedAt: '2026-05-20T12:59:00Z',
};

function page(
  content: EventResponse[],
  pageNumber: number,
  totalPages: number,
): PageResponse<EventResponse> {
  return { content, page: pageNumber, size: 20, totalElements: totalPages * 20, totalPages };
}

describe('EventList', () => {
  let fixture: ComponentFixture<EventList>;
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
    fixture = TestBed.createComponent(EventList);
    host = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function text(): string {
    return host.textContent!.replace(/\s+/g, ' ');
  }

  async function respond(pageNumber: number, body: object, status = 200): Promise<void> {
    const req = httpMock.expectOne(
      (r) => r.url === '/api/events' && r.params.get('page') === String(pageNumber),
    );
    expect(req.request.params.has('size')).toBe(false);
    req.flush(body, { status, statusText: String(status) });
    await fixture.whenStable();
  }

  function buttons(): Record<string, HTMLButtonElement> {
    return Object.fromEntries(
      [...host.querySelectorAll<HTMLButtonElement>('app-paginator button')].map((b) => [
        b.textContent!.trim(),
        b,
      ]),
    );
  }

  it('UI-09 given the request is in flight when the page renders then a loading message is shown', async () => {
    expect(host.querySelector('app-status-message')!.getAttribute('role')).toBe('status');
    expect(text()).toContain('Cargando eventos…');

    await respond(0, page([QUALITY_EVENT], 0, 1));
  });

  it('UI-09 given events when they load then each row shows friendly values and formatted dates', async () => {
    await respond(0, page([QUALITY_EVENT, PRODUCTION_EVENT], 0, 1));

    const rows = [...host.querySelectorAll('tbody tr')].map((row) =>
      [...row.querySelectorAll('td')].map((cell) => cell.textContent!.replace(/\s+/g, ' ').trim()),
    );
    expect(rows).toEqual([
      [
        'EVT-1002',
        'Calidad QUALITY',
        'Quality check started',
        '20/05/2026 10:00:00',
        formatDate(QUALITY_EVENT.receivedAt, 'dd/MM/yyyy HH:mm:ss', 'en-US'),
      ],
      [
        'EVT-1001',
        'Producción PRODUCTION',
        'Production line started',
        '20/05/2026 09:30:15',
        formatDate(PRODUCTION_EVENT.receivedAt, 'dd/MM/yyyy HH:mm:ss', 'en-US'),
      ],
    ]);
    expect(
      [...host.querySelectorAll('th[scope="col"]')].map((th) => th.textContent!.trim()),
    ).toEqual(['Identificador', 'Tipo', 'Descripción', 'Fecha del evento', 'Recibido']);
    expect(host.querySelector('td[data-label="Recibido"]')).not.toBeNull();
  });

  it('UI-09 given two pages when the user moves forward then the next page is requested and the edge buttons are disabled', async () => {
    await respond(0, page([QUALITY_EVENT], 0, 2));
    expect(buttons()['Anterior'].disabled).toBe(true);
    expect(buttons()['Siguiente'].disabled).toBe(false);
    expect(text()).toContain('Página 1 de 2');

    buttons()['Siguiente'].click();
    fixture.detectChanges();
    await respond(1, page([PRODUCTION_EVENT], 1, 2));

    expect(text()).toContain('Página 2 de 2');
    expect(buttons()['Anterior'].disabled).toBe(false);
    expect(buttons()['Siguiente'].disabled).toBe(true);
    expect(text()).toContain('EVT-1001');
  });

  it('UI-09 given the server cannot be reached when the page loads then the error is shown and retry requests again', async () => {
    httpMock.expectOne((r) => r.url === '/api/events').error(new ProgressEvent('error'));
    await fixture.whenStable();

    expect(host.querySelector('app-status-message')!.getAttribute('role')).toBe('alert');
    expect(text()).toContain(CONNECTION_ERROR_MESSAGE);

    host.querySelector<HTMLButtonElement>('app-status-message button')!.click();
    fixture.detectChanges();
    await respond(0, page([QUALITY_EVENT], 0, 1));

    expect(text()).toContain('EVT-1002');
  });

  it('UI-17 given no events when the page loads then it explains it and the button navigates to /events/new', async () => {
    await respond(0, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    expect(text()).toContain('Todavía no hay eventos registrados.');
    expect(host.querySelector('table')).toBeNull();
    const link = host.querySelector<HTMLAnchorElement>('a.button')!;
    expect(link.textContent!.trim()).toBe('Registrar evento');

    link.click();
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/events/new');
  });

  it('UI-17 given events of both types when they load then each row has the badge of its type', async () => {
    await respond(0, page([QUALITY_EVENT, PRODUCTION_EVENT], 0, 1));

    const badges = [...host.querySelectorAll('.badge')].map((b) => b.className);
    expect(badges).toEqual(['badge badge--quality', 'badge badge--production']);
  });
});
