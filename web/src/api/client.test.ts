import { afterEach, describe, expect, it, vi } from 'vitest';
import { mockFetch } from '../test/utils';
import { ApiError, api, onUnauthorized, setToken } from './client';

describe('api client', () => {
  afterEach(() => onUnauthorized(null));

  it('sends the stored token and parses json', async () => {
    setToken('abc');
    const calls = mockFetch({ '/api/me': { id: 1 } });
    const spy = vi.spyOn(globalThis, 'fetch');
    await expect(api.get('/api/me')).resolves.toEqual({ id: 1 });
    expect(calls).toHaveLength(1);
    const headers = spy.mock.calls[0][1]?.headers as Record<string, string>;
    expect(headers.Authorization).toBe('Bearer abc');
  });

  it('turns error bodies into ApiErrors with field messages', async () => {
    mockFetch({
      'POST /api/habits': () =>
        new Response(
          JSON.stringify({ status: 400, error: 'validation_failed', message: 'Some fields are invalid', fields: { name: 'Required' } }),
          { status: 400 },
        ),
    });
    const error = await api.post('/api/habits', {}).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).fields).toEqual({ name: 'Required' });
    expect((error as ApiError).code).toBe('validation_failed');
  });

  it('signs out when a stored token is rejected', async () => {
    setToken('expired');
    const handler = vi.fn();
    onUnauthorized(handler);
    mockFetch({ '/api/today': () => new Response(JSON.stringify({ error: 'unauthorized', message: 'Expired' }), { status: 401 }) });
    await expect(api.get('/api/today')).rejects.toBeInstanceOf(ApiError);
    expect(handler).toHaveBeenCalledOnce();
  });

  it('does not sign out on a failed login', async () => {
    const handler = vi.fn();
    onUnauthorized(handler);
    mockFetch({ 'POST /api/auth/login': () => new Response(JSON.stringify({ error: 'invalid_credentials' }), { status: 401 }) });
    await expect(api.post('/api/auth/login', {})).rejects.toBeInstanceOf(ApiError);
    expect(handler).not.toHaveBeenCalled();
  });

  it('reports network failures', async () => {
    globalThis.fetch = (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch;
    const error = (await api.get('/api/me').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('network_error');
  });

  it('returns undefined for 204 responses', async () => {
    mockFetch({ 'DELETE /api/habits/1': undefined });
    await expect(api.delete('/api/habits/1')).resolves.toBeUndefined();
  });
});
