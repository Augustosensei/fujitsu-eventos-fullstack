export const EVENT_ID_MAX_LENGTH = 64;

export const EVENT_ID_PATTERN = /^[A-Za-z0-9_-]+$/;

export const DESCRIPTION_MAX_LENGTH = 500;

const NON_BREAKING_SPACES = new Set(['\u00A0', '\u2007', '\u202F']);

/** Same characters as Java `Character.isWhitespace`, used by the backend's `strip()` and `isBlank()`. */
function isJavaWhitespace(char: string): boolean {
  const code = char.charCodeAt(0);
  const isInformationSeparator = code >= 0x1c && code <= 0x1f;
  return (
    isInformationSeparator || (/[\t\n\v\f\r\p{Z}]/u.test(char) && !NON_BREAKING_SPACES.has(char))
  );
}

export function strip(value: string): string {
  let start = 0;
  let end = value.length;
  while (start < end && isJavaWhitespace(value[start])) {
    start++;
  }
  while (end > start && isJavaWhitespace(value[end - 1])) {
    end--;
  }
  return value.slice(start, end);
}

export function isBlank(value: string): boolean {
  return strip(value) === '';
}
