import { Route, Routes } from 'react-router';
import { RequireAuth } from './auth/guards';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <h1>Habits</h1>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
