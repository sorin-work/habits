import { api } from './client';
import type {
  CheckIn,
  Habit,
  HabitInput,
  TodayResponse,
} from './types';

export const createHabit = (input: HabitInput) => api.post<Habit>('/api/habits', input);

export const fetchToday = () => api.get<TodayResponse>('/api/today');

export const saveCheckIn = (habitId: number, date: string, input: { value?: number; note?: string | null }) =>
  api.put<CheckIn>(`/api/habits/${habitId}/checkins/${date}`, input);

export const deleteCheckIn = (habitId: number, date: string) => api.delete(`/api/habits/${habitId}/checkins/${date}`);
