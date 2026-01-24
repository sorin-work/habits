import type { Bucket, StatsPeriod } from '../api/types';
import { formatMonth, formatShort } from '../lib/dates';
import { percent } from '../lib/format';

interface CompletionChartProps {
  buckets: Bucket[];
  period: StatsPeriod;
  color?: string;
}

const HEIGHT = 140;

/** Completion rate per week or month as a simple bar chart. */
export function CompletionChart({ buckets, period, color = 'var(--accent)' }: CompletionChartProps) {
  const label = (bucket: Bucket) => (period === 'week' ? formatShort(bucket.start) : formatMonth(bucket.start));
  return (
    <figure className="chart">
      <div className="chart-bars" style={{ height: HEIGHT }}>
        {buckets.map((bucket) => {
          const rate = bucket.rate ?? 0;
          const description =
            bucket.rate == null
              ? `${label(bucket)}: nothing scheduled`
              : `${label(bucket)}: ${percent(bucket.rate)} (${bucket.completed} of ${bucket.expected})`;
          return (
            <div className="chart-bar" key={bucket.start} title={description} aria-label={description} role="img">
              <div
                className={`chart-fill${bucket.rate == null ? ' empty' : ''}`}
                style={{ height: `${Math.max(rate * 100, bucket.rate == null ? 0 : 2)}%`, background: color }}
              />
            </div>
          );
        })}
      </div>
      <div className="chart-labels" aria-hidden="true">
        {buckets.map((bucket) => (
          <span key={bucket.start}>{label(bucket)}</span>
        ))}
      </div>
    </figure>
  );
}
