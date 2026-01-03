import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { listHabits } from '../api/habits';
import { ErrorMessage } from '../components/ErrorMessage';
import { describeSchedule, describeTarget } from '../lib/schedule';

export function HabitsPage() {
  const query = useQuery({ queryKey: ['habits'], queryFn: () => listHabits() });

  return (
    <div className="page">
      <div className="page-header">
        <h1>Habits</h1>
        <Link to="/habits/new" className="button primary">
          New habit
        </Link>
      </div>

      {query.isPending ? (
        <div className="page-loading">Loading…</div>
      ) : query.isError ? (
        <ErrorMessage error={query.error} />
      ) : query.data.length === 0 ? (
          <p className="muted">No habits yet.</p>
      ) : (
        <ul className="habit-list">
          {query.data.map((habit) => (
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
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
