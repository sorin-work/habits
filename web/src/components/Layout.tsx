import { NavLink, Outlet } from 'react-router';
import { useAuth } from '../auth/AuthContext';

const links = [
  { to: '/', label: 'Today', end: true },
  { to: '/habits', label: 'Habits', end: false },
  { to: '/stats', label: 'Stats', end: false },
];

export function Layout() {
  const { user, signOut } = useAuth();
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <img src="/favicon.svg" alt="" width={24} height={24} />
            Habits
          </NavLink>
          <nav className="nav" aria-label="Main">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end}>
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar-user">
            <span className="muted">{user?.name}</span>
            <button type="button" className="link-button" onClick={signOut}>
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
