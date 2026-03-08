import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { changePassword, updateProfile } from '../api/auth';
import { ApiError } from '../api/client';
import type { WeekStart } from '../api/types';
import { useAuth, useUser } from '../auth/AuthContext';
import { errorText } from '../components/ErrorMessage';
import { Field } from '../components/Field';

export function SettingsPage() {
  return (
    <div className="page narrow">
      <h1>Settings</h1>
      <ProfileForm />
      <PasswordForm />
    </div>
  );
}

function ProfileForm() {
  const user = useUser();
  const { setUser } = useAuth();
  const queryClient = useQueryClient();
  const [name, setName] = useState(user.name);
  const [timeZone, setTimeZone] = useState(user.timeZone);
  const [weekStart, setWeekStart] = useState<WeekStart>(user.weekStart);
  const save = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      setUser(updated);
      // Today, streaks and weeks all depend on the time zone and week start.
      void queryClient.invalidateQueries();
    },
  });
  const fields = save.error instanceof ApiError ? save.error.fields : {};

  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate({ name, timeZone, weekStart });
  };

  return (
    <form className="card form" onSubmit={submit}>
      <h2>Profile</h2>
      <Field label="Name" error={fields.name}>
        {(props) => <input {...props} value={name} maxLength={80} onChange={(e) => setName(e.target.value)} />}
      </Field>
      <Field label="Email">{(props) => <input {...props} value={user.email} disabled />}</Field>
      <Field label="Time zone" error={fields.timeZone} hint="Your days start and end at midnight in this time zone.">
        {(props) => <input {...props} value={timeZone} onChange={(e) => setTimeZone(e.target.value)} />}
      </Field>
      <Field label="Weeks start on" error={fields.weekStart}>
        {(props) => (
          <select {...props} value={weekStart} onChange={(e) => setWeekStart(e.target.value as WeekStart)}>
            <option value="MONDAY">Monday</option>
            <option value="SUNDAY">Sunday</option>
          </select>
        )}
      </Field>
      {save.isError && Object.keys(fields).length === 0 && (
        <div className="error-box" role="alert">
          {errorText(save.error)}
        </div>
      )}
      <div className="form-actions">
        <button type="submit" className="button primary" disabled={save.isPending}>
          Save profile
        </button>
      </div>
    </form>
  );
}

function PasswordForm() {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const save = useMutation({
    mutationFn: () => changePassword(current, next),
    onSuccess: () => {
      setCurrent('');
      setNext('');
    },
  });
  const fields = save.error instanceof ApiError ? save.error.fields : {};

  return (
    <form
      className="card form"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <h2>Password</h2>
      <Field label="Current password" error={fields.currentPassword}>
        {(props) => (
          <input
            {...props}
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
          />
        )}
      </Field>
      <Field label="New password" error={fields.newPassword} hint="At least 8 characters.">
        {(props) => (
          <input
            {...props}
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
          />
        )}
      </Field>
      <div className="form-actions">
        <button type="submit" className="button" disabled={save.isPending || !current || !next}>
          Change password
        </button>
      </div>
    </form>
  );
}
