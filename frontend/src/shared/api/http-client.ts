/**
 * The fetch mutator every generated hook goes through. Requests are same-origin relative URLs: the
 * Vite dev proxy and nginx forward `/api` to the backend, so there is no CORS and no base URL.
 * A fork that adds auth injects its `Authorization` header here.
 */
export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly problem: unknown,
  ) {
    super(`Request failed with status ${status}`);
    this.name = "HttpError";
  }
}

export const httpClient = async <T>(url: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(url, options);

  const text = await response.text();
  const body = text ? safeJsonParse(text) : undefined;

  if (!response.ok) {
    throw new HttpError(response.status, body);
  }

  return body as T;
};

const safeJsonParse = (text: string): unknown => {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
};

export default httpClient;
