import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import {
  archiveHabit,
  deleteHabit,
  fetchHabit,
  fetchHabitStats,
  fetchHeatmap,
  listCheckIns,
  restoreHabit,
} from '../api/habits';
import type { StatsPeriod } from '../api/types';
import { useUser } from '../auth/AuthContext';
import { CompletionChart } from '../components/CompletionChart';
import { ErrorMessage } from '../components/ErrorMessage';
import { HabitHeatmap } from '../components/HabitHeatmap';
import { formatLong } from '../lib/dates';
import { percent } from '../lib/format';
import { describeSchedule, describeTarget, streakLabel } from '../lib/schedule';

export function HabitDetailPage() {
  const id = Number(useParams().id);
  const user = useUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [period, setPeriod] = useState<StatsPeriod>('week');

  const detail = useQuery({ queryKey: ['habit', id], queryFn: () => fetchHabit(id) });
  const heatmap = useQuery({ queryKey: ['habit', id, 'heatmap'], queryFn: () => fetchHeatmap(id) });
  const stats = useQuery({ queryKey: ['habit', id, 'stats', period], queryFn: () => fetchHabitStats(id, period) });
  const checkIns = useQuery({ queryKey: ['habit', id, 'checkins'], queryFn: () => listCheckIns(id) });

  const afterChange = () => {
    void queryClient.invalidateQueries({ queryKey: ['habit', id] });
    void queryClient.invalidateQueries({ queryKey: ['habits'] });
    void queryClient.invalidateQueries({ queryKey: ['today'] });
    void queryClient.invalidateQueries({ queryKey: ['overview'] });
  };
  const archive = useMutation({ mutationFn: () => archiveHabit(id), onSuccess: afterChange });
  const restore = useMutation({ mutationFn: () => restoreHabit(id), onSuccess: afterChange });
  const remove = useMutation({
    mutationFn: () => deleteHabit(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['habit', id] });
      void queryClient.invalidateQueries({ queryKey: ['habits'] });
      void queryClient.invalidateQueries({ queryKey: ['today'] });
      navigate('/habits');
    },
  });

  if (detail.isPending) return <div className="page-loading">Loading…</div>;
  if (detail.isError) return <ErrorMessage error={detail.error} retry={() => void detail.refetch()} />;

  const { habit, summary } = detail.data;
  const target = describeTarget(habit);
  const notes = (checkIns.data ?? []).filter((c) => c.note);

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
            {habit.archived && ' · archived'}
          </p>
          {habit.description && <p>{habit.description}</p>}
        </div>
        <div className="header-actions">
          <Link to={`/habits/${habit.id}/edit`} className="button">
            Edit
          </Link>
          {habit.archived ? (
            <button type="button" className="button" disabled={restore.isPending} onClick={() => restore.mutate()}>
              Restore
            </button>
          ) : (
            <button type="button" className="button" disabled={archive.isPending} onClick={() => archive.mutate()}>
              Archive
            </button>
          )}
          <button
            type="button"
            className="button danger"
            disabled={remove.isPending}
            onClick={() => {
              if (window.confirm(`Delete "${habit.name}" and all of its check-ins? This can't be undone.`)) {
                remove.mutate();
              }
            }}
          >
            Delete
          </button>
        </div>
      </div>
      {(archive.error || restore.error || remove.error) && (
        <ErrorMessage error={archive.error ?? restore.error ?? remove.error} />
      )}

      <div className="stat-cards">
        <div className="stat-card">
          <span className="stat-label">Current streak</span>
          <span className="stat-value">{streakLabel(summary.streak.current, summary.streak.unit)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Best streak</span>
          <span className="stat-value">{streakLabel(summary.streak.best, summary.streak.unit)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Last 30 days</span>
          <span className="stat-value">{percent(summary.last30Days.rate)}</span>
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
            <span className="stat-label">{habit.target ? `Total ${habit.unit ?? ''}`.trim() : 'Times done'}</span>
            <span className="stat-value">
              {habit.target ? summary.totalValue.toLocaleString() : summary.totalCompleted.toLocaleString()}
            </span>
          </div>
        )}
      </div>

      <section className="card">
        <h2>History</h2>
        {heatmap.isPending ? (
          <div className="page-loading">Loading…</div>
        ) : heatmap.isError ? (
          <ErrorMessage error={heatmap.error} />
        ) : (
          <HabitHeatmap heatmap={heatmap.data} color={habit.color} weekStart={user.weekStart} />
        )}
      </section>

      <section className="card">
        <div className="card-header">
          <h2>Completion</h2>
          <div className="segmented small" role="group" aria-label="Period">
            {(['week', 'month'] as const).map((p) => (
              <button key={p} type="button" aria-pressed={period === p} onClick={() => setPeriod(p)}>
                {p === 'week' ? 'Weekly' : 'Monthly'}
              </button>
            ))}
          </div>
        </div>
        {stats.isPending ? (
          <div className="page-loading">Loading…</div>
        ) : stats.isError ? (
          <ErrorMessage error={stats.error} />
        ) : (
          <CompletionChart buckets={stats.data.buckets} period={period} color={habit.color} />
        )}
      </section>

      <section className="card">
        <h2>Notes</h2>
        {notes.length === 0 ? (
          <p className="muted">No notes in the last 90 days. Add one from the Today page after checking in.</p>
        ) : (
          <ul className="note-list">
            {notes.map((c) => (
              <li key={c.date}>
                <span className="muted small">{formatLong(c.date)}</span>
                <p>{c.note}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
