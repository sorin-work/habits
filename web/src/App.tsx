import { Route, Routes } from 'react-router';
import { GuestOnly, RequireAuth } from './auth/guards';
import { Layout } from './components/Layout';
import { EditHabitPage } from './pages/EditHabitPage';
import { HabitDetailPage } from './pages/HabitDetailPage';
import { HabitsPage } from './pages/HabitsPage';
import { LoginPage } from './pages/LoginPage';
import { NewHabitPage } from './pages/NewHabitPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { SettingsPage } from './pages/SettingsPage';
import { SignupPage } from './pages/SignupPage';
import { StatsPage } from './pages/StatsPage';
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
        <Route path="habits/:id/edit" element={<EditHabitPage />} />
        <Route path="stats" element={<StatsPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
