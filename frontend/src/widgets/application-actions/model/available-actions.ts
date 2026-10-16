import type { LeasingStatus } from "@/entities/leasing-application";

export type LeasingAction = "sign" | "handover" | "withdraw";

/**
 * The customer actions the process is waiting for in each status. A running case can always be
 * withdrawn; a withdrawn or finished case offers nothing. The back-office decisions (credit check,
 * reversal) live in the inbox, not here.
 */
const ACTIONS_BY_STATUS: Record<LeasingStatus, LeasingAction[]> = {
  RECEIVED: ["withdraw"],
  CONTRACT_SENT: ["sign", "withdraw"],
  ORDERED: ["handover", "withdraw"],
  ACTIVE: [],
  REJECTED: [],
  WITHDRAWN: [],
  CANCELLED: [],
};

export function availableActions(status: LeasingStatus | string): LeasingAction[] {
  return ACTIONS_BY_STATUS[status as LeasingStatus] ?? [];
}
