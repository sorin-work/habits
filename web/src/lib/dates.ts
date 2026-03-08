import type { WeekStart } from '../api/types';

/** Habit days are plain calendar dates ("2026-03-18") in the user's time zone, computed by the API. */
export function parseDate(iso: string): Date {
  return new Date(`${iso}T00:00:00`);
}

export function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function addDays(iso: string, days: number): string {
  const date = parseDate(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

/** ISO weekday: 1 = Monday ... 7 = Sunday. */
export function isoWeekday(iso: string): number {
  const day = parseDate(iso).getDay();
  return day === 0 ? 7 : day;
}

export function startOfWeek(iso: string, weekStart: WeekStart): string {
  const first = weekStart === 'SUNDAY' ? 7 : 1;
  const offset = (isoWeekday(iso) - first + 7) % 7;
  return addDays(iso, -offset);
}

const fullFormat = new Intl.DateTimeFormat('en', { weekday: 'long', month: 'long', day: 'numeric' });
const shortFormat = new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('en', { month: 'short' });

export const formatLong = (iso: string) => fullFormat.format(parseDate(iso));
export const formatShort = (iso: string) => shortFormat.format(parseDate(iso));
export const formatMonth = (iso: string) => monthFormat.format(parseDate(iso));

/** "Today", "Yesterday" or the full date. */
export function relativeDay(iso: string, today: string): string {
  if (iso === today) return 'Today';
  if (iso === addDays(today, -1)) return 'Yesterday';
  return formatLong(iso);
}

export const WEEKDAYS = [
  { value: 1, short: 'Mon', initial: 'M' },
  { value: 2, short: 'Tue', initial: 'T' },
  { value: 3, short: 'Wed', initial: 'W' },
  { value: 4, short: 'Thu', initial: 'T' },
  { value: 5, short: 'Fri', initial: 'F' },
  { value: 6, short: 'Sat', initial: 'S' },
  { value: 7, short: 'Sun', initial: 'S' },
];

/** Weekdays in display order for the user's week start. */
export function orderedWeekdays(weekStart: WeekStart) {
  return weekStart === 'SUNDAY' ? [WEEKDAYS[6], ...WEEKDAYS.slice(0, 6)] : WEEKDAYS;
}
