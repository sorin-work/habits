import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router';
import { deleteCheckIn, fetchToday, saveCheckIn } from '../api/habits';
import type { TodayHabit } from '../api/types';
import { CheckInControl } from '../components/CheckInControl';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { NoteEditor } from '../components/NoteEditor';
import { ProgressBar } from '../components/ProgressBar';
import { StreakBadge } from '../components/StreakBadge';
import { addDays, relativeDay } from '../lib/dates';
import { describeSchedule } from '../lib/schedule';

type SaveInput = { habitId: number; date: string; value?: number; note?: string | null };

export function TodayPage() {
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const date = params.get('date') ?? undefined;
  const query = useQuery({ queryKey: ['today', date ?? 'current'], queryFn: () => fetchToday(date) });

  const refresh = (habitId: number) => {
    void queryClient.invalidateQueries({ queryKey: ['today'] });
    void queryClient.invalidateQueries({ queryKey: ['habit', habitId] });
    void queryClient.invalidateQueries({ queryKey: ['overview'] });
  };
  const save = useMutation({
    mutationFn: (input: SaveInput) => saveCheckIn(input.habitId, input.date, { value: input.value, note: input.note }),
    onSuccess: (_data, input) => refresh(input.habitId),
  });
  const clear = useMutation({
    mutationFn: (input: { habitId: number; date: string }) => deleteCheckIn(input.habitId, input.date),
    onSuccess: (_data, input) => refresh(input.habitId),
  });

  if (query.isPending) return <div className="page-loading">Loading…</div>;
  if (query.isError) return <ErrorMessage error={query.error} retry={() => void query.refetch()} />;

  const data = query.data;
  const isToday = data.date === data.today;
  const goTo = (day: string) => setParams(day === data.today ? {} : { date: day });
  const due = data.habits.filter((h) => h.due);
  const notDue = data.habits.filter((h) => !h.due);
  const busy = save.isPending || clear.isPending;
  const error = save.error ?? clear.error;

  const renderItem = (item: TodayHabit) => {
    const { habit } = item;
    return (
      <li
        key={habit.id}
        className={`today-item${item.completed ? ' completed' : ''}`}
        style={{ ['--habit-color' as string]: habit.color }}
      >
        <div className="today-item-main">
          <span className="habit-dot" aria-hidden="true" />
          <div className="today-item-text">
            <Link to={`/habits/${habit.id}`} className="habit-name">
              {habit.name}
            </Link>
            <div className="muted small">
              {item.week ? `${item.week.done} of ${item.week.goal} this week` : describeSchedule(habit.schedule)}
              {item.streak.current > 0 && (
                <>
                  {' · '}
                  <StreakBadge streak={item.streak} />
                </>
              )}
            </div>
          </div>
          <CheckInControl
            habit={habit}
            value={item.value}
            completed={item.completed}
            disabled={busy}
            onSet={(value) => save.mutate({ habitId: habit.id, date: data.date, value, note: item.note })}
            onClear={() => clear.mutate({ habitId: habit.id, date: data.date })}
          />
        </div>
        {item.value > 0 && (
          <NoteEditor
            note={item.note}
            disabled={busy}
            onSave={(note) => save.mutateAsync({ habitId: habit.id, date: data.date, value: item.value, note })}
          />
        )}
      </li>
    );
  };

  return (
    <div className="page today-page">
      <div className="page-header">
        <div>
          <h1>{relativeDay(data.date, data.today)}</h1>
          {data.dueCount > 0 && (
            <p className="muted">
              {data.completedCount === data.dueCount
                ? 'All done. Nice work!'
                : `${data.completedCount} of ${data.dueCount} done`}
            </p>
          )}
        </div>
        <div className="day-nav">
          <button type="button" className="button" onClick={() => goTo(addDays(data.date, -1))} aria-label="Previous day">
            ←
          </button>
          {!isToday && (
            <button type="button" className="button" onClick={() => goTo(data.today)}>
              Today
            </button>
          )}
          <button
            type="button"
            className="button"
            onClick={() => goTo(addDays(data.date, 1))}
            disabled={isToday}
            aria-label="Next day"
          >
            →
          </button>
        </div>
      </div>

      {data.dueCount > 0 && <ProgressBar value={data.completedCount} max={data.dueCount} label="Today's progress" />}
      {error && <ErrorMessage error={error} />}

      {data.habits.length === 0 ? (
        <EmptyState title="No habits yet">
          <p>Start with something small you want to do every day.</p>
          <Link to="/habits/new" className="button primary">
            Add your first habit
          </Link>
        </EmptyState>
      ) : (
        <>
          <ul className="today-list">{due.map(renderItem)}</ul>
          {notDue.length > 0 && (
            <section className="not-due">
              <h2>Not due {isToday ? 'today' : 'this day'}</h2>
              <ul className="today-list">{notDue.map(renderItem)}</ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}
