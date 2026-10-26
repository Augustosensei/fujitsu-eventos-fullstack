export const EVENT_TYPES = ['PRODUCTION', 'QUALITY'] as const;

export type EventType = (typeof EVENT_TYPES)[number];

export const EVENT_TYPE_LABELS: Readonly<Record<EventType, string>> = {
  PRODUCTION: 'Producción',
  QUALITY: 'Calidad',
};

export const EVENT_TYPE_OPTIONS: readonly { readonly value: EventType; readonly label: string }[] =
  [
    { value: 'PRODUCTION', label: 'Producción (PRODUCTION)' },
    { value: 'QUALITY', label: 'Calidad (QUALITY): genera auditoría' },
  ];
