import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import {
  fetchHabit,
} from '../api/habits';
import { ErrorMessage } from '../components/ErrorMessage';
import { formatLong } from '../lib/dates';
import { describeSchedule, describeTarget, streakLabel } from '../lib/schedule';

export function HabitDetailPage() {
  const id = Number(useParams().id);

  const detail = useQuery({ queryKey: ['habit', id], queryFn: () => fetchHabit(id) });

  if (detail.isPending) return <div className="page-loading">Loading…</div>;
  if (detail.isError) return <ErrorMessage error={detail.error} retry={() => void detail.refetch()} />;

  const { habit, summary } = detail.data;
  const target = describeTarget(habit);

  return (
    <div className="page habit-detail" style={{ ['--habit-color' as string]: habit.color }}>
      <div className="page-header">
        <div>
          <h1>
            <span className="habit-dot large" aria-hidden="true" /> {habit.name}
          </h1>
          <p className="muted">
            {describeSchedule(habit.schedule)}
            {target && ` · ${target} a day`} · since {formatLong(habit.startDate)}
          </p>
        </div>
        <div className="header-actions">
          <Link to={`/habits/${habit.id}/edit`} className="button">
            Edit
          </Link>
        </div>
      </div>

      <div className="stat-cards">
        <div className="stat-card">
          <span className="stat-label">Current streak</span>
          <span className="stat-value">{streakLabel(summary.streak.current, summary.streak.unit)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Best streak</span>
          <span className="stat-value">{streakLabel(summary.streak.best, summary.streak.unit)}</span>
        </div>
        {summary.thisWeek ? (
          <div className="stat-card">
            <span className="stat-label">This week</span>
            <span className="stat-value">
              {summary.thisWeek.done} / {summary.thisWeek.goal}
            </span>
          </div>
        ) : (
          <div className="stat-card">
            <span className="stat-label">Check-ins</span>
            <span className="stat-value">{summary.totalCheckIns.toLocaleString()}</span>
          </div>
        )}
      </div>

    </div>
  );
}
