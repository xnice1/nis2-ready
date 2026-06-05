const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export type ApiOptions = RequestInit & { form?: FormData };

export function token() {
  return localStorage.getItem('token');
}

export function setToken(value: string | null) {
  if (value) localStorage.setItem('token', value);
  else localStorage.removeItem('token');
}

export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (!(options.body instanceof FormData) && !options.form) headers['Content-Type'] = 'application/json';
  const jwt = token();
  if (jwt) headers.Authorization = `Bearer ${jwt}`;
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    body: options.form ?? options.body,
    headers: { ...headers, ...(options.headers as Record<string, string> | undefined) }
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    if (res.status === 401) setToken(null);
    throw new Error(err.message ?? 'Request failed');
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}
