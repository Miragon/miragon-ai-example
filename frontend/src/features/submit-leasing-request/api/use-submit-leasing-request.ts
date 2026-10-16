import { useQueryClient } from "@tanstack/react-query";
import { toast } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useSubmitLeasingRequest } from "@/shared/api/generated/endpoints";
import { leasingApplicationKeys } from "@/entities/leasing-application";

export function useSubmitLeasingRequestAction(onCreated: (applicationId: string) => void) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  return useSubmitLeasingRequest({
    mutation: {
      onSuccess: async (created) => {
        await queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all });
        toast.success(copy.submit.successToast);
        onCreated(created.applicationId);
      },
      onError: () => toast.error(copy.submit.errorToast),
    },
  });
}
