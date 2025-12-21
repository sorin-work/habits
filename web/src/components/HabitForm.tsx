import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import type { Habit, HabitInput } from '../api/types';
import { ColorPicker } from './ColorPicker';
import { errorText } from './ErrorMessage';
import { Field } from './Field';

const DEFAULT_COLOR = '#4f7cff';

interface HabitFormProps {
  habit?: Habit;
  today: string;
  submitLabel: string;
  onSubmit: (input: HabitInput) => Promise<unknown>;
  onCancel?: () => void;
}

type Errors = Record<string, string>;

export function HabitForm({ habit, today, submitLabel, onSubmit, onCancel }: HabitFormProps) {
  const [name, setName] = useState(habit?.name ?? '');
  const [color, setColor] = useState(habit?.color ?? DEFAULT_COLOR);
  const [measured, setMeasured] = useState(habit?.target != null);
  const [target, setTarget] = useState(habit?.target != null ? String(habit.target) : '');
  const [unit, setUnit] = useState(habit?.unit ?? '');
  const [startDate, setStartDate] = useState(habit?.startDate ?? today);
  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const validate = (): Errors => {
    const next: Errors = {};
    if (!name.trim()) next.name = 'Give the habit a name';
    if (measured) {
      const value = Number(target);
      if (!Number.isInteger(value) || value < 1) next.target = 'Enter a whole number above zero';
    }
    if (!startDate) next.startDate = 'Pick a start date';
    return next;
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const found = validate();
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    const input: HabitInput = {
      name: name.trim(),
      description: null,
      color,
      schedule: { type: 'daily' },
      target: measured ? Number(target) : null,
      unit: measured && unit.trim() ? unit.trim() : null,
      startDate,
    };
    setSaving(true);
    try {
      await onSubmit(input);
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fields).length > 0) setErrors(error.fields);
      else setFormError(errorText(error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="form habit-form" onSubmit={submit} noValidate>
      {formError && (
        <div className="error-box" role="alert">
          {formError}
        </div>
      )}
      <Field label="Name" error={errors.name}>
        {(props) => (
          <input
            {...props}
            value={name}
            maxLength={80}
            placeholder="e.g. Drink water"
            onChange={(e) => setName(e.target.value)}
            autoFocus={!habit}
          />
        )}
      </Field>

      <div className="field">
        <span className="label">Colour</span>
        <ColorPicker value={color} onChange={setColor} />
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={measured} onChange={(e) => setMeasured(e.target.checked)} />
        Track an amount each day (e.g. 8 glasses, 30 minutes)
      </label>
      {measured && (
        <div className="field-row">
          <Field label="Daily target" error={errors.target}>
            {(props) => (
              <input
                {...props}
                type="number"
                inputMode="numeric"
                min={1}
                value={target}
                onChange={(e) => setTarget(e.target.value)}
              />
            )}
          </Field>
          <Field label="Unit (optional)" error={errors.unit}>
            {(props) => (
              <input
                {...props}
                value={unit}
                maxLength={20}
                placeholder="glasses"
                onChange={(e) => setUnit(e.target.value)}
              />
            )}
          </Field>
        </div>
      )}

      <Field
        label="Start date"
        error={errors.startDate}
        hint="Days before this don't count towards streaks or stats."
      >
        {(props) => <input {...props} type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />}
      </Field>

      <div className="form-actions">
        <button type="submit" className="button primary" disabled={saving}>
          {saving ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="button" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
