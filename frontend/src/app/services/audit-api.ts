import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Service, Signal } from '@angular/core';
import { API_PATHS } from '../models/api-paths';
import { AuditResponse } from '../models/audit';
import { PageQuery, PageResponse } from '../models/page';
import { toPageParams } from './page-params';

@Service()
export class AuditApi {
  /** Must be called in an injection context: the resource lives as long as the caller. */
  page(query: Signal<PageQuery>): HttpResourceRef<PageResponse<AuditResponse> | undefined> {
    return httpResource<PageResponse<AuditResponse>>(() => ({
      url: API_PATHS.audits,
      params: toPageParams(query()),
    }));
  }
}
