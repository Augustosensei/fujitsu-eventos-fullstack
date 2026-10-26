import { PageQuery } from '../models/page';

export function toPageParams({ page, size }: PageQuery): Record<string, number> {
  return size === undefined ? { page } : { page, size };
}
