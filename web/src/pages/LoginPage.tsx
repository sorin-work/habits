import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { login } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import { errorText } from '../components/ErrorMessage';
import { Field } from '../components/Field';

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      signIn(await login(email, password));
      navigate('/', { replace: true });
    } catch (e) {
      setError(errorText(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card form" onSubmit={submit}>
        <h1>Sign in</h1>
        <p className="muted">Keep your streaks going.</p>
        {error && (
          <div className="error-box" role="alert">
            {error}
          </div>
        )}
        <Field label="Email">
          {(props) => (
            <input
              {...props}
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          )}
        </Field>
        <Field label="Password">
          {(props) => (
            <input
              {...props}
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          )}
        </Field>
        <button type="submit" className="button primary wide" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="muted small">
          New here? <Link to="/signup">Create an account</Link>
        </p>
      </form>
    </div>
  );
}
