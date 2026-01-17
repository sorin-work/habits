import type { HeatmapDay } from '../api/types';
import { formatMonth, isoWeekday, parseDate } from './dates';

export interface HeatmapColumn {
  /** Seven slots, one per weekday in display order. Null where the range doesn't cover the day. */
  days: (HeatmapDay | null)[];
  /** Month label shown above the first column of each month. */
  label: string | null;
}

/** Arranges heatmap days into week columns, GitHub style. */
export function buildColumns(days: HeatmapDay[], weekStart: 'MONDAY' | 'SUNDAY'): HeatmapColumn[] {
  const first = weekStart === 'SUNDAY' ? 7 : 1;
  const columns: HeatmapColumn[] = [];
  let current: (HeatmapDay | null)[] = [];

  for (const day of days) {
    const slot = (isoWeekday(day.date) - first + 7) % 7;
    if (slot === 0 && current.length > 0) {
      columns.push({ days: pad(current), label: null });
      current = [];
    }
    while (current.length < slot) current.push(null);
    current.push(day);
  }
  if (current.length > 0) columns.push({ days: pad(current), label: null });

  let lastMonth = -1;
  for (const column of columns) {
    const firstDay = column.days.find((d) => d !== null);
    if (!firstDay) continue;
    const month = parseDate(firstDay.date).getMonth();
    if (month !== lastMonth) {
      column.label = formatMonth(firstDay.date);
      lastMonth = month;
    }
  }
  return columns;
}

function pad(days: (HeatmapDay | null)[]) {
  return [...days, ...Array<null>(7 - days.length).fill(null)];
}
