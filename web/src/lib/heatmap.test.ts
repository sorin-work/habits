import { describe, expect, it } from 'vitest';
import type { HeatmapDay } from '../api/types';
import { addDays } from './dates';
import { buildColumns } from './heatmap';

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
});
