import { HttpError } from "./http-client";

export function isConflict(error: unknown): boolean {
  return error instanceof HttpError && error.status === 409;
}

export function isNotFound(error: unknown): boolean {
  return error instanceof HttpError && error.status === 404;
}
