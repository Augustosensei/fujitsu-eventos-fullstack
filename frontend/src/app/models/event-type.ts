export const EVENT_TYPES = ['PRODUCTION', 'QUALITY'] as const;

export type EventType = (typeof EVENT_TYPES)[number];
