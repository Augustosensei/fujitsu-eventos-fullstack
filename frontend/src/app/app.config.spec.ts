import { FetchBackend, HttpBackend } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { appConfig } from './app.config';
import { routes } from './app.routes';

describe('appConfig', () => {
  it('UI-12 given the app config when the injector is built then HTTP goes through fetch and the router uses the app routes', () => {
    TestBed.configureTestingModule({ providers: appConfig.providers });

    expect(TestBed.inject(HttpBackend)).toBeInstanceOf(FetchBackend);
    expect(TestBed.inject(Router).config).toEqual(routes);
  });
});
