import { api } from './client';
import type { AuthResponse, User } from './types';

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { email, password });

export const register = (input: { email: string; password: string; name: string }) =>
  api.post<AuthResponse>('/api/auth/register', input);

export const fetchMe = () => api.get<User>('/api/me');
