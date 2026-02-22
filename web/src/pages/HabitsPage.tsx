import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router';
import { deleteHabit, listHabits, reorderHabits, restoreHabit } from '../api/habits';
import type { Habit } from '../api/types';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { describeSchedule, describeTarget } from '../lib/schedule';

export function HabitsPage() {
  const [params, setParams] = useSearchParams();
  const archived = params.get('view') === 'archived';
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['habits', archived], queryFn: () => listHabits(archived) });

  const reorder = useMutation({
    mutationFn: reorderHabits,
    onMutate: async (ids: number[]) => {
      await queryClient.cancelQueries({ queryKey: ['habits', false] });
      const previous = queryClient.getQueryData<Habit[]>(['habits', false]);
      if (previous) {
        const byId = new Map(previous.map((h) => [h.id, h]));
        queryClient.setQueryData(
          ['habits', false],
          ids.map((id) => byId.get(id)!),
        );
      }
      return { previous };
    },
    onError: (_error, _ids, context) => {
      if (context?.previous) queryClient.setQueryData(['habits', false], context.previous);
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['habits'] });
      void queryClient.invalidateQueries({ queryKey: ['today'] });
    },
  });

  const refreshAll = () => {
    void queryClient.invalidateQueries({ queryKey: ['habits'] });
    void queryClient.invalidateQueries({ queryKey: ['today'] });
    void queryClient.invalidateQueries({ queryKey: ['overview'] });
  };
  const restore = useMutation({ mutationFn: restoreHabit, onSuccess: refreshAll });
  const remove = useMutation({ mutationFn: deleteHabit, onSuccess: refreshAll });

  const move = (habits: Habit[], index: number, delta: number) => {
    const ids = habits.map((h) => h.id);
    const [moved] = ids.splice(index, 1);
    ids.splice(index + delta, 0, moved);
    reorder.mutate(ids);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Habits</h1>
        <Link to="/habits/new" className="button primary">
          New habit
        </Link>
      </div>
      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={!archived} onClick={() => setParams({})}>
          Active
        </button>
        <button type="button" role="tab" aria-selected={archived} onClick={() => setParams({ view: 'archived' })}>
          Archived
        </button>
      </div>

      {query.isPending ? (
        <div className="page-loading">Loading…</div>
      ) : query.isError ? (
        <ErrorMessage error={query.error} retry={() => void query.refetch()} />
      ) : query.data.length === 0 ? (
        archived ? (
          <EmptyState title="Nothing archived">
            <p>Archive a habit you've stopped to keep its history without seeing it every day.</p>
          </EmptyState>
        ) : (
          <p className="muted">No habits yet.</p>
        )
      ) : (
        <ul className="habit-list">
          {query.data.map((habit, index) => (
            <li key={habit.id} className="habit-row" style={{ ['--habit-color' as string]: habit.color }}>
              <span className="habit-dot" aria-hidden="true" />
              <div className="habit-row-text">
                <Link to={`/habits/${habit.id}`} className="habit-name">
                  {habit.name}
                </Link>
                <span className="muted small">
                  {describeSchedule(habit.schedule)}
                  {describeTarget(habit) && ` · ${describeTarget(habit)}`}
                </span>
              </div>
              {archived ? (
                <div className="row-actions">
                  <button
                    type="button"
                    className="button small"
                    disabled={restore.isPending}
                    onClick={() => restore.mutate(habit.id)}
                  >
                    Restore
                  </button>
                  <button
                    type="button"
                    className="button small danger"
                    disabled={remove.isPending}
                    onClick={() => {
                      if (window.confirm(`Delete "${habit.name}" and all of its check-ins? This can't be undone.`)) {
                        remove.mutate(habit.id);
                      }
                    }}
                  >
                    Delete
                  </button>
                </div>
              ) : (
                <div className="row-actions">
                  <button
                    type="button"
                    className="icon-button"
                    aria-label={`Move ${habit.name} up`}
                    disabled={index === 0 || reorder.isPending}
                    onClick={() => move(query.data, index, -1)}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    className="icon-button"
                    aria-label={`Move ${habit.name} down`}
                    disabled={index === query.data.length - 1 || reorder.isPending}
                    onClick={() => move(query.data, index, 1)}
                  >
                    ↓
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {(reorder.error || restore.error || remove.error) && (
        <ErrorMessage error={reorder.error ?? restore.error ?? remove.error} />
      )}
    </div>
  );
}
