import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { Service, Signal, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_PATHS } from '../models/api-paths';
import { EventRequest, EventResponse, RegisterResult } from '../models/event';
import { PageQuery, PageResponse } from '../models/page';
import { toPageParams } from './page-params';
import { toUiError } from './ui-error-mapper';

@Service()
export class EventApi {
  private readonly http = inject(HttpClient);

  async register(request: EventRequest): Promise<RegisterResult> {
    try {
      const event = await firstValueFrom(this.http.post<EventResponse>(API_PATHS.events, request));
      return { ok: true, event };
    } catch (error) {
      return { ok: false, error: toUiError(error) };
    }
  }

  /** Must be called in an injection context: the resource lives as long as the caller. */
  page(query: Signal<PageQuery>): HttpResourceRef<PageResponse<EventResponse> | undefined> {
    return httpResource<PageResponse<EventResponse>>(() => ({
      url: API_PATHS.events,
      params: toPageParams(query()),
    }));
  }
}
