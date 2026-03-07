import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { updateProfile } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth, useUser } from '../auth/AuthContext';
import { errorText } from '../components/ErrorMessage';
import { Field } from '../components/Field';

export function SettingsPage() {
  return (
    <div className="page narrow">
      <h1>Settings</h1>
      <ProfileForm />
    </div>
  );
}

function ProfileForm() {
  const user = useUser();
  const { setUser } = useAuth();
  const queryClient = useQueryClient();
  const [name, setName] = useState(user.name);
  const [timeZone, setTimeZone] = useState(user.timeZone);
  const save = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      setUser(updated);
      // Today and streaks depend on the time zone.
      void queryClient.invalidateQueries();
    },
  });
  const fields = save.error instanceof ApiError ? save.error.fields : {};

  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate({ name, timeZone });
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
