import type { Streak } from '../api/types';
import { streakLabel } from '../lib/schedule';

export function StreakBadge({ streak }: { streak: Streak }) {
  if (streak.current === 0) return null;
  return (
    <span className="streak-badge" title={`Best: ${streakLabel(streak.best, streak.unit)}`}>
      <svg className="flame" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
        <path d="M8 1c.5 2.6 3.8 4.2 3.8 8.1A3.8 3.8 0 0 1 8 13a3.8 3.8 0 0 1-3.8-3.9c0-1.6.8-2.8 1.7-3.6.1 1.2.7 2 1.4 2.2C7 6 6.9 3.3 8 1z" />
      </svg>{' '}
      {streakLabel(streak.current, streak.unit)}
    </span>
  );
}
