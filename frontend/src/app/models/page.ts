/** Rows per page in the UI tables; the API keeps its own default for direct clients (DT8). */
export const TABLE_PAGE_SIZE = 10;

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
