import {
  getGetLeasingApplicationQueryKey,
  getListLeasingApplicationsQueryKey,
} from "@/shared/api/generated/endpoints";
import type { ListLeasingApplicationsParams } from "@/shared/api/generated/model";

/**
 * orval emits the per-query keys; `all` adds the invalidation prefix shared by every list query
 * (pinned by keys.test.ts, so an orval bump cannot silently break invalidation).
 */
export const leasingApplicationKeys = {
  all: ["/api/bike-leasing"] as const,
  list: (params?: ListLeasingApplicationsParams) => getListLeasingApplicationsQueryKey(params),
  detail: (applicationId: string) => getGetLeasingApplicationQueryKey(applicationId),
};
