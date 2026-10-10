import { describe, expect, it } from 'vitest';
import type { HeatmapDay } from '../api/types';
import { addDays } from './dates';
import { buildColumns, describeDay } from './heatmap';

function range(from: string, count: number): HeatmapDay[] {
  return Array.from({ length: count }, (_, i) => ({
    date: addDays(from, i),
    value: 0,
    level: 0,
    scheduled: true,
    completed: false,
  }));
}

describe('buildColumns', () => {
  it('puts each week in its own column, padded at the edges', () => {
    // Wednesday 18 March to Tuesday 24 March.
    const columns = buildColumns(range('2026-03-18', 7), 'MONDAY');
    expect(columns).toHaveLength(2);
    expect(columns[0].days.map((d) => d?.date ?? null)).toEqual([
      null,
      null,
      '2026-03-18',
      '2026-03-19',
      '2026-03-20',
      '2026-03-21',
      '2026-03-22',
    ]);
    expect(columns[1].days[0]?.date).toBe('2026-03-23');
    expect(columns[1].days[2]).toBeNull();
  });

  it('starts columns on Sunday when weeks start on Sunday', () => {
    const columns = buildColumns(range('2026-03-15', 7), 'SUNDAY');
    expect(columns).toHaveLength(1);
    expect(columns[0].days[0]?.date).toBe('2026-03-15');
  });

  it('labels the first column of each month', () => {
    const columns = buildColumns(range('2026-02-02', 42), 'MONDAY');
    const labels = columns.map((c) => c.label);
    expect(labels[0]).toBe('Feb');
    expect(labels.filter(Boolean)).toEqual(['Feb', 'Mar']);
  });
});

describe('describeDay', () => {
  const day = { date: '2026-03-18', value: 5, level: 2, scheduled: true, completed: false };

  it('shows progress for measured habits', () => {
    expect(describeDay(day, 8, 'glasses')).toBe('2026-03-18: 5/8 glasses');
  });

  it('distinguishes missed days from days off', () => {
    expect(describeDay({ ...day, value: 0 }, 1, null)).toBe('2026-03-18: not done');
    expect(describeDay({ ...day, value: 0, scheduled: false }, 1, null)).toBe('2026-03-18: no check-in');
    expect(describeDay({ ...day, value: 1 }, 1, null)).toBe('2026-03-18: done');
  });
});
