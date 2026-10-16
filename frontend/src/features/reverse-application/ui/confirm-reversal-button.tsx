import { Button } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useCompleteReversalAction } from "../api/use-complete-reversal";

export function ConfirmReversalButton({
  applicationId,
  onCompleted,
}: {
  applicationId: string;
  onCompleted?: () => void;
}) {
  const copy = useCopy();
  const mutation = useCompleteReversalAction(applicationId, onCompleted);
  const busy = mutation.isPending || mutation.isSuccess;
  return (
    <Button variant="danger" onClick={() => mutation.mutate({ applicationId })} disabled={busy}>
      {copy.reversal.confirm}
    </Button>
  );
}
