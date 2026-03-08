import type { WeekStart } from '../api/types';
import { orderedWeekdays } from '../lib/dates';

interface WeekdayPickerProps {
  value: number[];
  onChange: (days: number[]) => void;
  weekStart: WeekStart;
}

export function WeekdayPicker({ value, onChange, weekStart }: WeekdayPickerProps) {
  const toggle = (day: number) => {
    onChange(value.includes(day) ? value.filter((d) => d !== day) : [...value, day]);
  };
  return (
    <div className="weekday-picker" role="group" aria-label="Days">
      {orderedWeekdays(weekStart).map((day) => (
        <button
          key={day.value}
          type="button"
          aria-pressed={value.includes(day.value)}
          className="weekday"
          onClick={() => toggle(day.value)}
        >
          {day.short}
        </button>
      ))}
    </div>
  );
}
