import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { isConflict } from "@/shared/api";
import { useSignContract } from "@/shared/api/generated/endpoints";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";

export function useSignContractAction(applicationId: string) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  const refreshApplication = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all }),
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.detail(applicationId) }),
      queryClient.invalidateQueries({ queryKey: openTaskKeys.all }),
    ]);
  return useSignContract({
    mutation: {
      onSuccess: async () => {
        await refreshApplication();
        toast.success(copy.actions.signSuccess);
      },
      onError: (error) => {
        if (!isConflict(error)) {
          toast.error(copy.actions.error);
          return;
        }
        void refreshApplication();
        toast.error(copy.common.conflictToast);
      },
    },
  });
}
