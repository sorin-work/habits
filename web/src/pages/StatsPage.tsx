import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { fetchOverview } from '../api/habits';
import type { StatsPeriod } from '../api/types';
import { useUser } from '../auth/AuthContext';
import { CompletionChart } from '../components/CompletionChart';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { orderedWeekdays } from '../lib/dates';
import { percent } from '../lib/format';
import { streakLabel } from '../lib/schedule';

export function StatsPage() {
  const user = useUser();
  const [period, setPeriod] = useState<StatsPeriod>('week');
  const query = useQuery({ queryKey: ['overview', period], queryFn: () => fetchOverview(period) });

  if (query.isPending) return <div className="page-loading">Loading…</div>;
  if (query.isError) return <ErrorMessage error={query.error} retry={() => void query.refetch()} />;

  const data = query.data;
  if (data.habits.length === 0) {
    return (
      <div className="page">
        <h1>Stats</h1>
        <EmptyState title="Nothing to show yet">
          <p>Stats appear once you have a habit and a few check-ins.</p>
          <Link to="/habits/new" className="button primary">
            Add a habit
          </Link>
        </EmptyState>
      </div>
    );
  }

  const byDay = new Map(data.weekdays.map((d) => [d.day, d]));
  const current = data.buckets[data.buckets.length - 1];

  return (
    <div className="page">
      <div className="page-header">
        <h1>Stats</h1>
        <div className="segmented small" role="group" aria-label="Period">
          {(['week', 'month'] as const).map((p) => (
            <button key={p} type="button" aria-pressed={period === p} onClick={() => setPeriod(p)}>
              {p === 'week' ? 'Weekly' : 'Monthly'}
            </button>
          ))}
        </div>
      </div>

      <section className="card">
        <div className="card-header">
          <h2>All habits</h2>
          <span className="muted">
            This {period}: {percent(current?.rate)}
          </span>
        </div>
        <CompletionChart buckets={data.buckets} period={period} />
      </section>

      <section className="card">
        <h2>By habit</h2>
        <table className="table">
          <thead>
            <tr>
              <th scope="col">Habit</th>
              <th scope="col">Last 30 days</th>
              <th scope="col">Current streak</th>
              <th scope="col">Best streak</th>
            </tr>
          </thead>
          <tbody>
            {[...data.habits]
              .sort((a, b) => (b.last30Days.rate ?? -1) - (a.last30Days.rate ?? -1))
              .map((habit) => (
                <tr key={habit.id} style={{ ['--habit-color' as string]: habit.color }}>
                  <td>
                    <span className="habit-dot" aria-hidden="true" /> <Link to={`/habits/${habit.id}`}>{habit.name}</Link>
                  </td>
                  <td>{percent(habit.last30Days.rate)}</td>
                  <td>{streakLabel(habit.streak.current, habit.streak.unit)}</td>
                  <td>{streakLabel(habit.streak.best, habit.streak.unit)}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>Best days</h2>
        <p className="muted small">Completion of scheduled check-ins by weekday over the last 12 weeks.</p>
        <ul className="weekday-bars">
          {orderedWeekdays(user.weekStart).map((day) => {
            const entry = byDay.get(day.value);
            return (
              <li key={day.value}>
                <span className="weekday-label">{day.short}</span>
                <span className="weekday-track">
                  <span className="weekday-fill" style={{ width: `${(entry?.rate ?? 0) * 100}%` }} />
                </span>
                <span className="weekday-value">{percent(entry?.rate)}</span>
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}
