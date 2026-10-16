import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useWithdrawApplication } from "@/shared/api/generated/endpoints";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";

export function useWithdrawApplicationAction(applicationId: string) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  return useWithdrawApplication({
    mutation: {
      onSuccess: async () => {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all }),
          queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.detail(applicationId) }),
          queryClient.invalidateQueries({ queryKey: openTaskKeys.all }),
        ]);
        toast.success(copy.actions.withdrawSuccess);
      },
      onError: () => toast.error(copy.actions.error),
    },
  });
}
