import { getListOpenTasksQueryKey, useListOpenTasks } from "@/shared/api/generated/endpoints";

export { useListOpenTasks };

export const openTaskKeys = {
  all: getListOpenTasksQueryKey(),
};
