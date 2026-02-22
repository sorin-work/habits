import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { habit, mockFetch, renderWithProviders } from '../test/utils';
import { HabitsPage } from './HabitsPage';

const habits = [habit({ id: 1, name: 'Read' }), habit({ id: 2, name: 'Run' }), habit({ id: 3, name: 'Water' })];

describe('HabitsPage', () => {
  it('moves a habit down', async () => {
    const calls = mockFetch({
      '/api/habits?archived=false': habits,
      'PUT /api/habits/order': [habits[1], habits[0], habits[2]],
    });
    renderWithProviders(<HabitsPage />);
    expect(await screen.findByRole('button', { name: 'Move Read up' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Move Read down' }));
    expect(calls.find((c) => c.method === 'PUT')?.body).toEqual({ ids: [2, 1, 3] });
  });

  it('shows archived habits with restore', async () => {
    const calls = mockFetch({
      '/api/habits?archived=true': [habit({ id: 9, name: 'Spanish', archived: true })],
      'POST /api/habits/9/restore': habit({ id: 9, name: 'Spanish' }),
    });
    renderWithProviders(<HabitsPage />, { route: '/habits?view=archived' });
    await userEvent.click(await screen.findByRole('button', { name: 'Restore' }));
    expect(calls.some((c) => c.method === 'POST' && c.path === '/api/habits/9/restore')).toBe(true);
  });
});
