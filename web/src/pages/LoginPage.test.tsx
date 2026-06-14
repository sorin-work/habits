import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { getToken } from '../api/client';
import { AuthProvider } from '../auth/AuthContext';
import { mockFetch, renderWithProviders } from '../test/utils';
import { LoginPage } from './LoginPage';

const user = {
  id: 1,
  email: 'demo@example.com',
  name: 'Alex',
  timeZone: 'Europe/Berlin',
  weekStart: 'MONDAY',
  createdAt: '2026-01-01T00:00:00Z',
};

function renderLogin() {
  return renderWithProviders(
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<p>Today page</p>} />
      </Routes>
    </AuthProvider>,
    { route: '/login' },
  );
}

describe('LoginPage', () => {
  it('signs in and goes to today', async () => {
    const calls = mockFetch({ 'POST /api/auth/login': { token: 'tok', expiresAt: '2026-04-01T00:00:00Z', user } });
    renderLogin();
    await userEvent.type(screen.getByLabelText('Email'), 'demo@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'demo-password');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByText('Today page')).toBeInTheDocument();
    expect(getToken()).toBe('tok');
    expect(calls[0].body).toEqual({ email: 'demo@example.com', password: 'demo-password' });
  });

  it('shows the error from the api', async () => {
    mockFetch({
      'POST /api/auth/login': () =>
        new Response(JSON.stringify({ error: 'invalid_credentials', message: 'Email or password is incorrect' }), {
          status: 401,
        }),
    });
    renderLogin();
    await userEvent.type(screen.getByLabelText('Email'), 'demo@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'nope');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect');
    expect(getToken()).toBeNull();
  });
});
