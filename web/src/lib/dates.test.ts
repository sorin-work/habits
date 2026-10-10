import { describe, expect, it } from 'vitest';
import { addDays, isoWeekday, orderedWeekdays, relativeDay, startOfWeek } from './dates';

describe('dates', () => {
  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-02-28', 1)).toBe('2026-03-01');
    expect(addDays('2025-12-31', 1)).toBe('2026-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('keeps calendar dates stable across DST changes', () => {
    // Europe switches to summer time on 29 March 2026.
    expect(addDays('2026-03-28', 1)).toBe('2026-03-29');
    expect(addDays('2026-03-29', 1)).toBe('2026-03-30');
  });

  it('numbers weekdays like ISO', () => {
    expect(isoWeekday('2026-03-16')).toBe(1);
    expect(isoWeekday('2026-03-22')).toBe(7);
  });

  it('finds the start of the week for either week start', () => {
    expect(startOfWeek('2026-03-18', 'MONDAY')).toBe('2026-03-16');
    expect(startOfWeek('2026-03-18', 'SUNDAY')).toBe('2026-03-15');
    expect(startOfWeek('2026-03-15', 'SUNDAY')).toBe('2026-03-15');
    expect(startOfWeek('2026-03-15', 'MONDAY')).toBe('2026-03-09');
  });

  it('orders weekdays for the week start', () => {
    expect(orderedWeekdays('MONDAY').map((d) => d.short)[0]).toBe('Mon');
    expect(orderedWeekdays('SUNDAY').map((d) => d.short)).toEqual(['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']);
  });

  it('describes days relative to today', () => {
    expect(relativeDay('2026-03-18', '2026-03-18')).toBe('Today');
    expect(relativeDay('2026-03-17', '2026-03-18')).toBe('Yesterday');
    expect(relativeDay('2026-03-10', '2026-03-18')).toBe('Tuesday, March 10');
  });
});
