import { api } from './client';
import type {
  CheckIn,
  Habit,
  HabitDetail,
  HabitInput,
  TodayResponse,
} from './types';

export const listHabits = () => api.get<Habit[]>('/api/habits');

export const fetchHabit = (id: number) => api.get<HabitDetail>(`/api/habits/${id}`);

export const createHabit = (input: HabitInput) => api.post<Habit>('/api/habits', input);

export const updateHabit = (id: number, input: HabitInput) => api.put<Habit>(`/api/habits/${id}`, input);

export const fetchToday = () => api.get<TodayResponse>('/api/today');

export const saveCheckIn = (habitId: number, date: string, input: { value?: number; note?: string | null }) =>
  api.put<CheckIn>(`/api/habits/${habitId}/checkins/${date}`, input);

export const deleteCheckIn = (habitId: number, date: string) => api.delete(`/api/habits/${habitId}/checkins/${date}`);
