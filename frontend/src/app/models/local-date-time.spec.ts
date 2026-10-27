import { toLocalDateTimeInput } from './local-date-time';

describe('toLocalDateTimeInput', () => {
  it('UI-21 given a local date with one-digit parts and milliseconds when formatted then every part is zero-padded and the milliseconds are dropped', () => {
    expect(toLocalDateTimeInput(new Date(2026, 0, 2, 3, 4, 5, 678))).toBe('2026-01-02T03:04:05');
  });

  it('UI-21 given the last second of a year in local time when formatted then the local parts are kept', () => {
    expect(toLocalDateTimeInput(new Date(2026, 11, 31, 23, 59, 59, 999))).toBe(
      '2026-12-31T23:59:59',
    );
  });
});
