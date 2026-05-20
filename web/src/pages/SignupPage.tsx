import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { register } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { errorText } from '../components/ErrorMessage';
import { Field } from '../components/Field';
import { browserTimeZone } from '../lib/dates';

export function SignupPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    const found: Record<string, string> = {};
    if (!name.trim()) found.name = 'Enter your name';
    if (password.length < 8) found.password = 'Password must be at least 8 characters';
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      signIn(await register({ name, email, password, timeZone: browserTimeZone() }));
      navigate('/habits/new', { replace: true });
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fields).length > 0) setErrors(e.fields);
      else setError(errorText(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card form" onSubmit={submit} noValidate>
        <h1>Create an account</h1>
        <p className="muted">Small steps, every day.</p>
        {error && (
          <div className="error-box" role="alert">
            {error}
          </div>
        )}
        <Field label="Name" error={errors.name}>
          {(props) => <input {...props} autoComplete="name" value={name} onChange={(e) => setName(e.target.value)} />}
        </Field>
        <Field label="Email" error={errors.email}>
          {(props) => (
            <input {...props} type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          )}
        </Field>
        <Field label="Password" error={errors.password} hint="At least 8 characters.">
          {(props) => (
            <input
              {...props}
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          )}
        </Field>
        <button type="submit" className="button primary wide" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
        <p className="muted small">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
