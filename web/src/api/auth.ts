import { api } from './client';
import type { AuthResponse, User, WeekStart } from './types';

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { email, password });

export const register = (input: { email: string; password: string; name: string; timeZone: string }) =>
  api.post<AuthResponse>('/api/auth/register', input);

export const fetchMe = () => api.get<User>('/api/me');

export const updateProfile = (input: { name: string; timeZone: string; weekStart: WeekStart }) =>
  api.put<User>('/api/me', input);

export const changePassword = (currentPassword: string, newPassword: string) =>
  api.put<void>('/api/me/password', { currentPassword, newPassword });

export const deleteAccount = (password: string) => api.delete('/api/me', { password });
