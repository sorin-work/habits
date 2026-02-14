import { api } from './client';
import type {
  CheckIn,
  Habit,
  HabitDetail,
  HabitInput,
  HabitStats,
  Heatmap,
  Overview,
  StatsPeriod,
  TodayResponse,
} from './types';

export const listHabits = (archived = false) => api.get<Habit[]>(`/api/habits?archived=${archived}`);

export const fetchHabit = (id: number) => api.get<HabitDetail>(`/api/habits/${id}`);

export const createHabit = (input: HabitInput) => api.post<Habit>('/api/habits', input);

export const updateHabit = (id: number, input: HabitInput) => api.put<Habit>(`/api/habits/${id}`, input);

export const deleteHabit = (id: number) => api.delete(`/api/habits/${id}`);

export const archiveHabit = (id: number) => api.post<Habit>(`/api/habits/${id}/archive`);

export const restoreHabit = (id: number) => api.post<Habit>(`/api/habits/${id}/restore`);

export const fetchToday = () => api.get<TodayResponse>('/api/today');

export const saveCheckIn = (habitId: number, date: string, input: { value?: number; note?: string | null }) =>
  api.put<CheckIn>(`/api/habits/${habitId}/checkins/${date}`, input);

export const deleteCheckIn = (habitId: number, date: string) => api.delete(`/api/habits/${habitId}/checkins/${date}`);

export const fetchHeatmap = (habitId: number) => api.get<Heatmap>(`/api/habits/${habitId}/heatmap`);

export const fetchHabitStats = (habitId: number, period: StatsPeriod, count = 12) =>
  api.get<HabitStats>(`/api/habits/${habitId}/stats?period=${period}&count=${count}`);

export const fetchOverview = (period: StatsPeriod, count = 12) =>
  api.get<Overview>(`/api/stats/overview?period=${period}&count=${count}`);
