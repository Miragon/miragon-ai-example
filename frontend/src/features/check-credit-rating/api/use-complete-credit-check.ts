import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useCompleteCreditCheck } from "@/shared/api/generated/endpoints";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";

export function useCompleteCreditCheckAction(applicationId: string, onCompleted?: () => void) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  return useCompleteCreditCheck({
    mutation: {
      onSuccess: async () => {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all }),
          queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.detail(applicationId) }),
          queryClient.invalidateQueries({ queryKey: openTaskKeys.all }),
        ]);
        toast.success(copy.creditCheck.successToast);
        onCompleted?.();
      },
      onError: () => toast.error(copy.creditCheck.errorToast),
    },
  });
}
