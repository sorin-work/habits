import type { Heatmap, WeekStart } from '../api/types';
import { orderedWeekdays } from '../lib/dates';
import { buildColumns } from '../lib/heatmap';

interface HabitHeatmapProps {
  heatmap: Heatmap;
  color: string;
  weekStart: WeekStart;
}

const OPACITY = [0, 0.3, 0.5, 0.75, 1];

/** A year of check-ins, one square per day, one column per week. */
export function HabitHeatmap({ heatmap, color, weekStart }: HabitHeatmapProps) {
  const columns = buildColumns(heatmap.days, weekStart);
  const weekdays = orderedWeekdays(weekStart);
  return (
    <div className="heatmap" style={{ ['--habit-color' as string]: color }}>
      <div className="heatmap-weekdays" aria-hidden="true">
        <span />
        {weekdays.map((d, i) => (
          <span key={d.value}>{i % 2 === 0 ? d.short : ''}</span>
        ))}
      </div>
      <div className="heatmap-scroll">
        <div className="heatmap-grid" role="grid" aria-label="Check-in history">
          {columns.map((column, i) => (
            <div className="heatmap-column" key={i} role="row">
              <span className="heatmap-month" aria-hidden="true">
                {column.label ?? ''}
              </span>
              {column.days.map((day, j) =>
                day ? (
                  <span
                    key={day.date}
                    role="gridcell"
                    className={`heatmap-cell${day.scheduled ? '' : ' off'}`}
                    data-level={day.level}
                    style={day.level > 0 ? { opacity: OPACITY[day.level] } : undefined}
                    title={day.date}
                  />
                ) : (
                  <span key={`empty-${j}`} className="heatmap-cell empty" aria-hidden="true" />
                ),
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
