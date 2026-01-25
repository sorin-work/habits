import { describe, expect, it } from 'vitest';
import { addDays, relativeDay } from './dates';

describe('dates', () => {
  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-02-28', 1)).toBe('2026-03-01');
    expect(addDays('2025-12-31', 1)).toBe('2026-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('describes days relative to today', () => {
    expect(relativeDay('2026-03-18', '2026-03-18')).toBe('Today');
    expect(relativeDay('2026-03-17', '2026-03-18')).toBe('Yesterday');
    expect(relativeDay('2026-03-10', '2026-03-18')).toBe('Tuesday, March 10');
  });
});
