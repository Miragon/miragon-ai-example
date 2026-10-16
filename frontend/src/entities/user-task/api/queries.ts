import { getListOpenTasksQueryKey, useListOpenTasks } from "@/shared/api/generated/endpoints";

export { useListOpenTasks };
export type { OpenTaskDto } from "@/shared/api/generated/model";

export const openTaskKeys = {
  all: getListOpenTasksQueryKey(),
};
