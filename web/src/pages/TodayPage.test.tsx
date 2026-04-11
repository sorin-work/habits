import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { TodayResponse } from '../api/types';
import { habit, mockFetch, renderWithProviders } from '../test/utils';
import { TodayPage } from './TodayPage';

const streak = { current: 0, best: 0, unit: 'days' as const };

function today(overrides: Partial<TodayResponse> = {}): TodayResponse {
  return {
    date: '2026-03-18',
    today: '2026-03-18',
    dueCount: 2,
    completedCount: 1,
    habits: [
      {
        habit: habit({ id: 1, name: 'Meditate' }),
        due: true,
        value: 1,
        note: 'Calm',
        completed: true,
        streak: { ...streak, current: 4, best: 9 },
        week: null,
      },
      {
        habit: habit({ id: 2, name: 'Water', target: 8, unit: 'glasses' }),
        due: true,
        value: 3,
        note: null,
        completed: false,
        streak,
        week: null,
      },
      {
        habit: habit({ id: 3, name: 'Swim', schedule: { type: 'weekdays', days: [2, 4] } }),
        due: false,
        value: 0,
        note: null,
        completed: false,
        streak,
        week: null,
      },
    ],
    ...overrides,
  };
}

describe('TodayPage', () => {
  it('lists due habits and those not due today', async () => {
    mockFetch({ '/api/today': today() });
    renderWithProviders(<TodayPage />);
    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument();
    expect(screen.getByText('1 of 2 done')).toBeInTheDocument();
    expect(screen.getByText(/4 days/)).toBeInTheDocument();
    expect(screen.getByText('Calm')).toBeInTheDocument();
    const notDue = screen.getByRole('heading', { name: 'Not due today' }).parentElement!;
    expect(within(notDue).getByText('Swim')).toBeInTheDocument();
  });

  it('saves progress for the shown day', async () => {
    const calls = mockFetch({
      '/api/today': today(),
      'PUT /api/habits/2/checkins/2026-03-18': { date: '2026-03-18', value: 4, note: null, completed: false, updatedAt: '' },
    });
    renderWithProviders(<TodayPage />);
    await userEvent.click(await screen.findByRole('button', { name: 'Increase Water' }));
    expect(calls.find((c) => c.method === 'PUT')).toEqual({
      method: 'PUT',
      path: '/api/habits/2/checkins/2026-03-18',
      body: { value: 4, note: null },
    });
  });

  it('can go back to an earlier day', async () => {
    const calls = mockFetch({
      '/api/today': today(),
      '/api/today?date=2026-03-17': today({ date: '2026-03-17' }),
    });
    renderWithProviders(<TodayPage />);
    await userEvent.click(await screen.findByRole('button', { name: 'Previous day' }));
    expect(await screen.findByRole('heading', { name: 'Yesterday' })).toBeInTheDocument();
    expect(calls.map((c) => c.path)).toContain('/api/today?date=2026-03-17');
    expect(screen.getByRole('button', { name: 'Today' })).toBeInTheDocument();
  });
});
