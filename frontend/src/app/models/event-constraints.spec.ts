import {
  DESCRIPTION_MAX_LENGTH,
  EVENT_ID_MAX_LENGTH,
  EVENT_ID_PATTERN,
  isBlank,
  strip,
} from './event-constraints';

describe('event constraints', () => {
  it('UI-01 given the D6 and D7 limits when read then eventId allows 64 characters and description 500', () => {
    expect(EVENT_ID_MAX_LENGTH).toBe(64);
    expect(DESCRIPTION_MAX_LENGTH).toBe(500);
  });

  it.each(['EVT-1001', 'evt_1001', 'A', '0-_'])(
    'UI-01 given the eventId %s when matched against the D6 pattern then it is accepted',
    (eventId) => {
      expect(EVENT_ID_PATTERN.test(eventId)).toBe(true);
    },
  );

  it.each(['', 'EVT 1001', 'ÉVT-1', 'EVT.1001', 'EVT/1001', ' EVT-1001'])(
    "UI-01 given the eventId '%s' when matched against the D6 pattern then it is rejected",
    (eventId) => {
      expect(EVENT_ID_PATTERN.test(eventId)).toBe(false);
    },
  );

  it.each([
    ['\t EVT-1001  ', 'EVT-1001'],
    ['\n\r\f\v\u001CEVT\u001F', 'EVT'],
    ['\u2003\u3000EVT\u2028\u2029', 'EVT'],
    ['EVT 1001', 'EVT 1001'],
  ])(
    "UI-19 given %j when stripped like Java String.strip then it becomes '%s'",
    (raw, expected) => {
      expect(strip(raw)).toBe(expected);
    },
  );

  it.each(['\u00A0EVT', 'EVT\u2007', '\u202FEVT'])(
    'UI-19 given the non-breaking space in %j when stripped then it is kept, as in Java',
    (raw) => {
      expect(strip(raw)).toBe(raw);
    },
  );

  it('UI-01 given only whitespace when checked with isBlank then it is blank, like the backend @NotBlank', () => {
    expect(isBlank(' \t\n ')).toBe(true);
    expect(isBlank('')).toBe(true);
    expect(isBlank('\u00A0')).toBe(false);
    expect(isBlank(' a ')).toBe(false);
  });
});
