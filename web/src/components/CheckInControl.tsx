import type { Habit } from '../api/types';

interface CheckInControlProps {
  habit: Pick<Habit, 'name' | 'target' | 'unit'>;
  value: number;
  completed: boolean;
  disabled?: boolean;
  onSet: (value: number) => void;
  onClear: () => void;
}

/**
 * A done/not done toggle for simple habits, or a counter with a target for measured ones.
 */
export function CheckInControl({ habit, value, completed, disabled, onSet, onClear }: CheckInControlProps) {
  if (habit.target == null) {
    return (
      <button
        type="button"
        className={`check-button${completed ? ' done' : ''}`}
        aria-label={completed ? `Undo ${habit.name}` : `Mark ${habit.name} as done`}
        disabled={disabled}
        onClick={() => (completed ? onClear() : onSet(1))}
      >
        <span aria-hidden="true">{completed ? '✓' : ''}</span>
      </button>
    );
  }

  const step = (delta: number) => onSet(Math.max(0, value + delta));

  return (
    <div className="counter">
      <button
        type="button"
        className="counter-step"
        aria-label={`Decrease ${habit.name}`}
        disabled={disabled || value === 0}
        onClick={() => step(-1)}
      >
        −
      </button>
      <span className={`counter-value${completed ? ' done' : ''}`}>
        {value} / {habit.target}
        {habit.unit && <span className="muted"> {habit.unit}</span>}
      </span>
      <button
        type="button"
        className="counter-step"
        aria-label={`Increase ${habit.name}`}
        disabled={disabled}
        onClick={() => step(1)}
      >
        +
      </button>
      {!completed && (
        <button
          type="button"
          className="button small"
          disabled={disabled}
          onClick={() => onSet(habit.target!)}
          aria-label={`Complete ${habit.name}`}
        >
          Done
        </button>
      )}
    </div>
  );
}
