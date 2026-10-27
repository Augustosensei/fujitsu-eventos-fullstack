function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** `datetime-local` value with seconds (`YYYY-MM-DDTHH:mm:ss`) in the browser's time zone. */
export function toLocalDateTimeInput(date: Date): string {
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  return `${day}T${time}`;
}
