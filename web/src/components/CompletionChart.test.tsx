import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { CompletionChart } from './CompletionChart';

describe('CompletionChart', () => {
  it('describes each bar', () => {
    render(
      <CompletionChart
        period="week"
        buckets={[
          { start: '2026-03-02', end: '2026-03-08', expected: 0, completed: 0, rate: null },
          { start: '2026-03-09', end: '2026-03-15', expected: 7, completed: 5, rate: 5 / 7 },
          { start: '2026-03-16', end: '2026-03-22', expected: 2, completed: 2, rate: 1 },
        ]}
      />,
    );
    expect(screen.getByLabelText('Mar 2: nothing scheduled')).toBeInTheDocument();
    expect(screen.getByLabelText('Mar 9: 71% (5 of 7)')).toBeInTheDocument();
    expect(screen.getByLabelText('Mar 16: 100% (2 of 2)')).toBeInTheDocument();
  });
});
