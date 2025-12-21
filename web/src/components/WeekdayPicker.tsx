import { WEEKDAYS } from '../lib/dates';

interface WeekdayPickerProps {
  value: number[];
  onChange: (days: number[]) => void;
}

export function WeekdayPicker({ value, onChange }: WeekdayPickerProps) {
  const toggle = (day: number) => {
    onChange(value.includes(day) ? value.filter((d) => d !== day) : [...value, day]);
  };
  return (
    <div className="weekday-picker" role="group" aria-label="Days">
      {WEEKDAYS.map((day) => (
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
