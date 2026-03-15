import { describe, expect, it, vi } from 'vitest';
import { mockFetch } from '../test/utils';
import { ApiError, api, setToken } from './client';

describe('api client', () => {

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

  it('returns undefined for 204 responses', async () => {
    mockFetch({ 'DELETE /api/habits/1': undefined });
    await expect(api.delete('/api/habits/1')).resolves.toBeUndefined();
  });
});
