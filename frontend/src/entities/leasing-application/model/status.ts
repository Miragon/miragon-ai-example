import type { Copy } from "@/shared/i18n";

export const LEASING_STATUSES = [
  "RECEIVED",
  "CONTRACT_SENT",
  "CONTRACT_SIGNED",
  "ORDERED",
  "ACTIVE",
  "REJECTED",
  "WITHDRAWN",
  "CANCELLED",
] as const;

export type LeasingStatus = (typeof LEASING_STATUSES)[number];

export type StatusTone = "neutral" | "success" | "warning" | "danger" | "info";

const TERMINAL_STATUSES: readonly LeasingStatus[] = ["ACTIVE", "REJECTED", "CANCELLED"];

/** The single definition of "the engine is done with this case"; views stop polling on it. */
export function isTerminal(status: LeasingStatus | string | undefined | null): boolean {
  return status != null && TERMINAL_STATUSES.includes(status as LeasingStatus);
}

export function statusLabel(status: LeasingStatus | string, copy: Copy): string {
  return copy.status[status as LeasingStatus] ?? status;
}

const TONES: Record<LeasingStatus, StatusTone> = {
  RECEIVED: "info",
  CONTRACT_SENT: "info",
  CONTRACT_SIGNED: "info",
  ORDERED: "info",
  ACTIVE: "success",
  REJECTED: "danger",
  WITHDRAWN: "warning",
  CANCELLED: "warning",
};

export function statusTone(status: LeasingStatus | string): StatusTone {
  return TONES[status as LeasingStatus] ?? "neutral";
}
