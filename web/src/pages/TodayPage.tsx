import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router';
import { deleteCheckIn, fetchToday, saveCheckIn } from '../api/habits';
import type { TodayHabit, TodayResponse } from '../api/types';
import { CheckInControl } from '../components/CheckInControl';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { NoteEditor } from '../components/NoteEditor';
import { ProgressBar } from '../components/ProgressBar';
import { StreakBadge } from '../components/StreakBadge';
import { addDays, relativeDay } from '../lib/dates';
import { describeSchedule } from '../lib/schedule';

export function TodayPage() {
  const [params, setParams] = useSearchParams();
  const date = params.get('date') ?? undefined;
  const query = useQuery({ queryKey: ['today', date ?? 'current'], queryFn: () => fetchToday(date) });

  if (query.isPending) return <div className="page-loading">Loading…</div>;
  if (query.isError) return <ErrorMessage error={query.error} retry={() => void query.refetch()} />;

  const data = query.data;
  const isToday = data.date === data.today;
  const goTo = (day: string) => setParams(day === data.today ? {} : { date: day });
  const due = data.habits.filter((h) => h.due);
  const notDue = data.habits.filter((h) => !h.due);

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

      {data.habits.length === 0 ? (
        <EmptyState title="No habits yet">
          <p>Start with something small you want to do every day.</p>
          <Link to="/habits/new" className="button primary">
            Add your first habit
          </Link>
        </EmptyState>
      ) : (
        <>
          <ul className="today-list">
            {due.map((item) => (
              <TodayItem key={item.habit.id} item={item} data={data} />
            ))}
          </ul>
          {notDue.length > 0 && (
            <section className="not-due">
              <h2>Not due {isToday ? 'today' : 'this day'}</h2>
              <ul className="today-list">
                {notDue.map((item) => (
                  <TodayItem key={item.habit.id} item={item} data={data} />
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}

function TodayItem({ item, data }: { item: TodayHabit; data: TodayResponse }) {
  const queryClient = useQueryClient();
  const { habit } = item;
  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['today'] });
    void queryClient.invalidateQueries({ queryKey: ['habit', habit.id] });
    void queryClient.invalidateQueries({ queryKey: ['overview'] });
  };
  const save = useMutation({
    mutationFn: (input: { value?: number; note?: string | null }) => saveCheckIn(habit.id, data.date, input),
    onSuccess: refresh,
  });
  const clear = useMutation({ mutationFn: () => deleteCheckIn(habit.id, data.date), onSuccess: refresh });
  const busy = save.isPending || clear.isPending;
  const error = save.error ?? clear.error;

  return (
    <li className={`today-item${item.completed ? ' completed' : ''}`} style={{ ['--habit-color' as string]: habit.color }}>
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
          onSet={(value) => save.mutate({ value, note: item.note })}
          onClear={() => clear.mutate()}
        />
      </div>
      {item.value > 0 && (
        <NoteEditor
          note={item.note}
          disabled={busy}
          onSave={(note) => save.mutateAsync({ value: item.value, note })}
        />
      )}
      {error && <ErrorMessage error={error} />}
    </li>
  );
}
