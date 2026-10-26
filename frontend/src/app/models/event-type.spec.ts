import { EVENT_TYPES } from './event-type';

describe('EVENT_TYPES', () => {
  it('UI-12 given the backend EventType enum when the types are listed then they are exactly PRODUCTION and QUALITY', () => {
    expect(EVENT_TYPES).toEqual(['PRODUCTION', 'QUALITY']);
  });
});
