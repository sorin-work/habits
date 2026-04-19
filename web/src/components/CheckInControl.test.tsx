import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { CheckInControl } from './CheckInControl';

describe('CheckInControl', () => {
  it('toggles a simple habit', async () => {
    const onSet = vi.fn();
    const onClear = vi.fn();
    const { rerender } = render(
      <CheckInControl habit={{ name: 'Meditate', target: null, unit: null }} value={0} completed={false} onSet={onSet} onClear={onClear} />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Mark Meditate as done' }));
    expect(onSet).toHaveBeenCalledWith(1);

    rerender(
      <CheckInControl habit={{ name: 'Meditate', target: null, unit: null }} value={1} completed onSet={onSet} onClear={onClear} />,
    );
    const undo = screen.getByRole('button', { name: 'Undo Meditate' });
    await userEvent.click(undo);
    expect(onClear).toHaveBeenCalled();
  });

  it('counts towards a target', async () => {
    const onSet = vi.fn();
    const onClear = vi.fn();
    render(
      <CheckInControl
        habit={{ name: 'Water', target: 8, unit: 'glasses' }}
        value={5}
        completed={false}
        onSet={onSet}
        onClear={onClear}
      />,
    );
    expect(screen.getByText(/5 \/ 8/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Increase Water' }));
    expect(onSet).toHaveBeenLastCalledWith(6);
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Water' }));
    expect(onSet).toHaveBeenLastCalledWith(4);
    await userEvent.click(screen.getByRole('button', { name: 'Complete Water' }));
    expect(onSet).toHaveBeenLastCalledWith(8);
  });

  it('clears the check-in when counting down to zero', async () => {
    const onSet = vi.fn();
    const onClear = vi.fn();
    render(
      <CheckInControl habit={{ name: 'Water', target: 8, unit: null }} value={1} completed={false} onSet={onSet} onClear={onClear} />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Water' }));
    expect(onClear).toHaveBeenCalled();
    expect(onSet).not.toHaveBeenCalled();
  });

  it('hides the done shortcut once the target is reached', () => {
    render(
      <CheckInControl habit={{ name: 'Water', target: 8, unit: null }} value={8} completed onSet={vi.fn()} onClear={vi.fn()} />,
    );
    expect(screen.queryByRole('button', { name: 'Complete Water' })).not.toBeInTheDocument();
  });
});
