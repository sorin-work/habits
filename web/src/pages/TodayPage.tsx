import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router';
import { deleteCheckIn, fetchToday, saveCheckIn } from '../api/habits';
import type { TodayHabit } from '../api/types';
import { CheckInControl } from '../components/CheckInControl';
import { ErrorMessage } from '../components/ErrorMessage';
import { NoteEditor } from '../components/NoteEditor';
import { describeSchedule, streakLabel } from '../lib/schedule';

type SaveInput = { habitId: number; date: string; value?: number; note?: string | null };

export function TodayPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['today'], queryFn: () => fetchToday() });

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
              {item.streak.current > 0 && ` · ${streakLabel(item.streak.current, item.streak.unit)} streak`}
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
            key={`${habit.id}-${item.note ?? ''}`}
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
          <h1>Today</h1>
          {data.dueCount > 0 && (
            <p className="muted">
              {`${data.completedCount} of ${data.dueCount} done`}
            </p>
          )}
        </div>
      </div>

      {error && <ErrorMessage error={error} />}

      {data.habits.length === 0 ? (
        <p className="muted">No habits yet. Add one from the Habits page.</p>
      ) : (
        <>
          <ul className="today-list">{data.habits.map(renderItem)}</ul>
        </>
      )}
    </div>
  );
}
