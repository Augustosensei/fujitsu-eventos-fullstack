import { Routes } from '@angular/router';

export const APP_NAME = 'Eventos de fábrica';

export const routes: Routes = [
  {
    path: 'events/new',
    title: `Registrar evento · ${APP_NAME}`,
    loadComponent: () =>
      import('./pages/event-register/event-register').then((m) => m.EventRegister),
  },
  {
    path: 'events',
    title: `Eventos registrados · ${APP_NAME}`,
    loadComponent: () => import('./pages/event-list/event-list').then((m) => m.EventList),
  },
  {
    path: 'audits',
    title: `Auditorías · ${APP_NAME}`,
    loadComponent: () => import('./pages/audit-list/audit-list').then((m) => m.AuditList),
  },
  { path: '', pathMatch: 'full', redirectTo: 'events/new' },
  { path: '**', redirectTo: 'events/new' },
];
