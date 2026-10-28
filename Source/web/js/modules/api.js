/**
 * API Module — Communicates with the WordBook Java SQLite REST Server.
 */
export async function fetchApi(path, options = {}) {
  try {
    const res = await fetch(path, {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `HTTP ${res.status}`);
    }
    const json = await res.json();
    return json.data !== undefined ? json.data : json;
  } catch (e) {
    console.warn(`[API] Call to ${path} failed:`, e.message);
    throw e;
  }
}
