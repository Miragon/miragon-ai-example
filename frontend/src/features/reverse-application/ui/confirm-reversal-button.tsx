import { Button } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useCompleteReversalAction } from "../api/use-complete-reversal";

export function ConfirmReversalButton({
  applicationId,
  onDone,
}: {
  applicationId: string;
  onDone?: () => void;
}) {
  const copy = useCopy();
  const mutation = useCompleteReversalAction(applicationId, onDone);
  const busy = mutation.isPending || mutation.isSuccess;
  return (
    <Button variant="danger" onClick={() => mutation.mutate({ applicationId })} disabled={busy}>
      {copy.reversal.confirm}
    </Button>
  );
}
