const TOKEN_KEY = 'habits.token';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fields: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

let unauthorizedHandler: (() => void) | null = null;

/** Called whenever the API rejects the stored token, e.g. because it expired. */
export function onUnauthorized(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

export async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'network_error', 'Could not reach the server. Check your connection.');
  }

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const data = text ? safeParse(text) : null;

  if (!response.ok) {
    const error = (data ?? {}) as { error?: string; message?: string; fields?: Record<string, string> };
    if (response.status === 401 && token) unauthorizedHandler?.();
    throw new ApiError(
      response.status,
      error.error ?? 'http_error',
      error.message ?? `Request failed (${response.status})`,
      error.fields ?? {},
    );
  }
  return data as T;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T = void>(path: string, body?: unknown) => request<T>('DELETE', path, body),
};
