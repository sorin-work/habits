export type ScheduleType = 'daily' | 'weekdays' | 'weekly';

export interface Schedule {
  type: ScheduleType;
  /** ISO weekday numbers, 1 = Monday ... 7 = Sunday. Only for `weekdays`. */
  days?: number[] | null;
  /** Only for `weekly`. */
  timesPerWeek?: number | null;
}

export interface Habit {
  id: number;
  name: string;
  description: string | null;
  color: string;
  schedule: Schedule;
  target: number | null;
  unit: string | null;
  startDate: string;
  createdAt: string;
}

export interface HabitInput {
  name: string;
  description: string | null;
  color: string;
  schedule: Schedule;
  target: number | null;
  unit: string | null;
  startDate: string;
}

export interface Streak {
  current: number;
  best: number;
  unit: 'days' | 'weeks';
}

export interface WeekProgress {
  done: number;
  goal: number;
}

export interface Completion {
  expected: number;
  completed: number;
  rate: number | null;
}

export interface HabitSummary {
  streak: Streak;
  last30Days: Completion;
  totalCheckIns: number;
  thisWeek: WeekProgress | null;
}

export interface HabitDetail {
  habit: Habit;
  summary: HabitSummary;
}

export interface CheckIn {
  date: string;
  value: number;
  note: string | null;
  completed: boolean;
  updatedAt: string;
}

export interface TodayHabit {
  habit: Habit;
  due: boolean;
  value: number;
  note: string | null;
  completed: boolean;
  streak: Streak;
  week: WeekProgress | null;
}

export interface TodayResponse {
  date: string;
  habits: TodayHabit[];
  dueCount: number;
  completedCount: number;
}

export type StatsPeriod = 'week' | 'month';

export interface Bucket extends Completion {
  start: string;
  end: string;
}

export interface HabitStats {
  period: StatsPeriod;
  buckets: Bucket[];
}

export interface HeatmapDay {
  date: string;
  value: number;
  level: number;
  scheduled: boolean;
  completed: boolean;
}

export interface Heatmap {
  from: string;
  to: string;
  goal: number;
  days: HeatmapDay[];
}

export interface User {
  id: number;
  email: string;
  name: string;
  timeZone: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}
