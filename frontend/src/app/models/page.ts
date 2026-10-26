export interface PageQuery {
  readonly page: number;
  /** When omitted, the backend applies its configured default size (DT8). */
  readonly size?: number;
}

export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
