import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HabitHeatmap } from './HabitHeatmap';

describe('HabitHeatmap', () => {
  it('renders a cell per day with its progress', () => {
    render(
      <HabitHeatmap
        color="#3fa7d6"
        unit="glasses"
        weekStart="MONDAY"
        heatmap={{
          from: '2026-03-16',
          to: '2026-03-18',
          goal: 8,
          days: [
            { date: '2026-03-16', value: 8, level: 4, scheduled: true, completed: true },
            { date: '2026-03-17', value: 4, level: 2, scheduled: true, completed: false },
            { date: '2026-03-18', value: 0, level: 0, scheduled: true, completed: false },
          ],
        }}
      />,
    );
    const cells = screen.getAllByRole('gridcell');
    expect(cells).toHaveLength(3);
    expect(cells[0]).toHaveAttribute('data-level', '4');
    expect(screen.getByLabelText('2026-03-17: 4/8 glasses')).toBeInTheDocument();
  });
});
