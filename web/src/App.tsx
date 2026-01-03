import { Route, Routes } from 'react-router';
import { GuestOnly, RequireAuth } from './auth/guards';
import { Layout } from './components/Layout';
import { HabitDetailPage } from './pages/HabitDetailPage';
import { HabitsPage } from './pages/HabitsPage';
import { LoginPage } from './pages/LoginPage';
import { NewHabitPage } from './pages/NewHabitPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SignupPage } from './pages/SignupPage';
import { TodayPage } from './pages/TodayPage';

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
        <Route index element={<TodayPage />} />
        <Route path="habits" element={<HabitsPage />} />
        <Route path="habits/new" element={<NewHabitPage />} />
        <Route path="habits/:id" element={<HabitDetailPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
