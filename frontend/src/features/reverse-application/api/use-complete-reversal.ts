import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { isConflict } from "@/shared/api";
import { useCompleteReversal } from "@/shared/api/generated/endpoints";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";

export function useCompleteReversalAction(applicationId: string, onDone?: () => void) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  const refreshApplication = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all }),
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.detail(applicationId) }),
      queryClient.invalidateQueries({ queryKey: openTaskKeys.all }),
    ]);
  return useCompleteReversal({
    mutation: {
      onSuccess: async () => {
        await refreshApplication();
        toast.success(copy.reversal.successToast);
        onDone?.();
      },
      onError: (error) => {
        if (!isConflict(error)) {
          toast.error(copy.reversal.errorToast);
          return;
        }
        void refreshApplication();
        toast.error(copy.common.conflictToast);
        onDone?.();
      },
    },
  });
}
