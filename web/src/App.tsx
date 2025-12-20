import { Route, Routes } from 'react-router';
import { GuestOnly, RequireAuth } from './auth/guards';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SignupPage } from './pages/SignupPage';

export function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <GuestOnly>
            <LoginPage />
          </GuestOnly>
        }
      />
      <Route
        path="/signup"
        element={
          <GuestOnly>
            <SignupPage />
          </GuestOnly>
        }
      />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<p>Nothing here yet.</p>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
