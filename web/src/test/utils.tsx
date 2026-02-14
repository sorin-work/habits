import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';
import type { Habit } from '../api/types';

/** Renders with a fresh query client (no retries) and a memory router. */
export function renderWithProviders(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </QueryClientProvider>,
    ),
  };
}

/** Responds to fetch calls by path. Unmatched paths fail the test loudly. */
export function mockFetch(routes: Record<string, unknown | ((init?: RequestInit) => unknown)>) {
  const calls: { method: string; path: string; body: unknown }[] = [];
  const fetchMock = async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = String(input);
    const method = init?.method ?? 'GET';
    calls.push({ method, path, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    const key = Object.keys(routes).find((k) => k === `${method} ${path}` || k === path);
    if (!key) return new Response(JSON.stringify({ error: 'not_mocked', message: path }), { status: 500 });
    const handler = routes[key];
    const body = typeof handler === 'function' ? handler(init) : handler;
    if (body instanceof Response) return body;
    if (body === undefined) return new Response(null, { status: 204 });
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  globalThis.fetch = fetchMock as typeof fetch;
  return calls;
}

export function habit(overrides: Partial<Habit> = {}): Habit {
  return {
    id: 1,
    name: 'Read',
    description: null,
    color: '#8e6cef',
    schedule: { type: 'daily' },
    target: null,
    unit: null,
    startDate: '2026-03-01',
    archived: false,
    archivedAt: null,
    createdAt: '2026-03-01T08:00:00Z',
    ...overrides,
  };
}
