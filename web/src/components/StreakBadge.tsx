import type { Streak } from '../api/types';
import { streakLabel } from '../lib/schedule';

export function StreakBadge({ streak }: { streak: Streak }) {
  if (streak.current === 0) return null;
  return (
    <span className="streak-badge" title={`Best: ${streakLabel(streak.best, streak.unit)}`}>
      <span aria-hidden="true">🔥</span>{' '}
      {streakLabel(streak.current, streak.unit)}
    </span>
  );
}
