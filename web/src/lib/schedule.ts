import type { Habit, Schedule } from '../api/types';
import { WEEKDAYS } from './dates';

export function describeSchedule(schedule: Schedule): string {
  switch (schedule.type) {
    case 'daily':
      return 'Every day';
    case 'weekly': {
      const times = schedule.timesPerWeek ?? 1;
      if (times === 7) return '7 times a week';
      return times === 1 ? 'Once a week' : `${times} times a week`;
    }
    case 'weekdays': {
      const days = [...(schedule.days ?? [])].sort((a, b) => a - b);
      if (days.length === 7) return 'Every day';
      if (days.join() === '1,2,3,4,5') return 'Weekdays';
      if (days.join() === '6,7') return 'Weekends';
      return days.map((d) => WEEKDAYS[d - 1].short).join(', ');
    }
  }
}

/** "8 glasses" or "30" for measured habits, null for simple done/not done habits. */
export function describeTarget(habit: Pick<Habit, 'target' | 'unit'>): string | null {
  if (habit.target == null) return null;
  return habit.unit ? `${habit.target} ${habit.unit}` : String(habit.target);
}

export function streakLabel(count: number, unit: 'days' | 'weeks'): string {
  const singular = unit === 'days' ? 'day' : 'week';
  return `${count} ${count === 1 ? singular : unit}`;
}
