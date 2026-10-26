import { EVENT_TYPE_LABELS, EVENT_TYPE_OPTIONS, EVENT_TYPES } from './event-type';

describe('EVENT_TYPES', () => {
  it('UI-12 given the backend EventType enum when the types are listed then they are exactly PRODUCTION and QUALITY', () => {
    expect(EVENT_TYPES).toEqual(['PRODUCTION', 'QUALITY']);
  });

  it('UI-14 given each type when it is shown then it has a friendly label and the selector keeps the technical value', () => {
    expect(EVENT_TYPE_LABELS).toEqual({ PRODUCTION: 'Producción', QUALITY: 'Calidad' });
    expect(EVENT_TYPE_OPTIONS.map((option) => option.value)).toEqual([...EVENT_TYPES]);
  });
});
