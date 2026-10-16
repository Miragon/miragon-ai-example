/**
 * How often a view re-reads a case or the inbox while the engine is still working. Every action
 * returns 202 and the engine advances asynchronously, so an invalidation alone often re-reads a
 * still-stale state; polling closes that gap.
 */
export const POLL_INTERVAL_MS = 2000;
