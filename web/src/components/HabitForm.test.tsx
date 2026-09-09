import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { habit } from '../test/utils';
import { HabitForm } from './HabitForm';

function setup(onSubmit = vi.fn().mockResolvedValue(undefined), existing?: ReturnType<typeof habit>) {
  render(<HabitForm habit={existing} today="2026-03-18" weekStart="MONDAY" submitLabel="Save" onSubmit={onSubmit} />);
  return onSubmit;
}

describe('HabitForm', () => {
  it('submits a daily habit with defaults', async () => {
    const onSubmit = setup();
    await userEvent.type(screen.getByLabelText('Name'), 'Stretch');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Stretch',
      description: null,
      color: '#4f7cff',
      schedule: { type: 'daily' },
      target: null,
      unit: null,
      startDate: '2026-03-18',
    });
  });

  it('submits specific days with a target', async () => {
    const onSubmit = setup();
    await userEvent.type(screen.getByLabelText('Name'), 'Run');
    await userEvent.click(screen.getByLabelText('Specific days'));
    // Mon, Wed and Fri are preselected; drop Wednesday and add Tuesday.
    await userEvent.click(screen.getByRole('button', { name: 'Wed' }));
    await userEvent.click(screen.getByRole('button', { name: 'Tue' }));
    await userEvent.click(screen.getByLabelText(/Track an amount/));
    await userEvent.type(screen.getByLabelText('Daily target'), '5');
    await userEvent.type(screen.getByLabelText('Unit (optional)'), 'km');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ schedule: { type: 'weekdays', days: [1, 2, 5] }, target: 5, unit: 'km' }),
    );
  });

  it('submits a weekly goal', async () => {
    const onSubmit = setup();
    await userEvent.type(screen.getByLabelText('Name'), 'Gym');
    await userEvent.click(screen.getByLabelText('Times per week'));
    await userEvent.selectOptions(screen.getByRole('combobox'), '4');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ schedule: { type: 'weekly', timesPerWeek: 4 } }));
  });

  it('checks the form before submitting', async () => {
    const onSubmit = setup();
    await userEvent.click(screen.getByLabelText('Specific days'));
    for (const day of ['Mon', 'Wed', 'Fri']) await userEvent.click(screen.getByRole('button', { name: day }));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(screen.getByText('Give the habit a name')).toBeInTheDocument();
    expect(screen.getByText('Pick at least one day')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows field errors from the api', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError(400, 'validation_failed', 'Some fields are invalid', { startDate: 'Start date can be at most a year ahead' }),
    );
    setup(onSubmit);
    await userEvent.type(screen.getByLabelText('Name'), 'Later');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Start date can be at most a year ahead')).toBeInTheDocument();
  });

  it('starts from an existing habit', () => {
    setup(vi.fn(), habit({ name: 'Water', target: 8, unit: 'glasses', schedule: { type: 'weekly', timesPerWeek: 2 } }));
    expect(screen.getByLabelText('Name')).toHaveValue('Water');
    expect(screen.getByLabelText('Daily target')).toHaveValue(8);
    expect(screen.getByLabelText('Times per week')).toBeChecked();
  });
});
