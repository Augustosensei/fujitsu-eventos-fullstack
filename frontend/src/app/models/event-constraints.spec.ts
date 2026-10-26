import { DESCRIPTION_MAX_LENGTH, EVENT_ID_MAX_LENGTH, EVENT_ID_PATTERN } from './event-constraints';

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
});
