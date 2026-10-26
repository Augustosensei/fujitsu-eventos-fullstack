import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let router: Router;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(App);
    router = TestBed.inject(Router);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  async function navigate(url: string): Promise<void> {
    await router.navigateByUrl(url);
    fixture.detectChanges();
    httpMock.match(() => true).forEach((req) => req.flush(EMPTY_PAGE));
    await fixture.whenStable();
  }

  function activeLinks(): string[] {
    return [
      ...(fixture.nativeElement as HTMLElement).querySelectorAll('.app-nav__link.is-active'),
    ].map((link) => `${link.textContent!.trim()} ${link.getAttribute('aria-current')}`);
  }

  it.each(['/', '/does-not-exist', '/events/new/extra'])(
    'UI-11 given the url %s when navigating then it redirects to /events/new',
    async (url) => {
      await navigate(url);

      expect(router.url).toBe('/events/new');
    },
  );

  it('UI-11 given each section when navigating then only its menu link is active with aria-current', async () => {
    await navigate('/events/new');
    expect(activeLinks()).toEqual(['Registrar page']);

    await navigate('/events');
    expect(activeLinks()).toEqual(['Eventos page']);

    await navigate('/audits');
    expect(activeLinks()).toEqual(['Auditorías page']);
  });

  it('UI-11 given the shell when it renders then the menu has the three sections and the page is in the router outlet', async () => {
    await navigate('/events/new');
    const host = fixture.nativeElement as HTMLElement;

    const links = [...host.querySelectorAll('.app-nav__link')].map((a) => [
      a.textContent!.trim(),
      a.getAttribute('href'),
    ]);
    expect(links).toEqual([
      ['Registrar', '/events/new'],
      ['Eventos', '/events'],
      ['Auditorías', '/audits'],
    ]);
    expect(host.querySelector('main router-outlet + app-event-register')).not.toBeNull();
  });

  it('UI-18 given each route when navigating then the browser tab gets its own title', async () => {
    const title = TestBed.inject(Title);
    const titles: string[] = [];

    for (const url of ['/events/new', '/events', '/audits']) {
      await navigate(url);
      titles.push(title.getTitle());
    }

    expect(titles).toEqual([
      'Registrar evento · Eventos de fábrica',
      'Eventos registrados · Eventos de fábrica',
      'Auditorías · Eventos de fábrica',
    ]);
  });
});
