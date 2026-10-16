import { Button } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useCompleteCreditCheckAction } from "../api/use-complete-credit-check";

export function CreditRatingDecision({
  applicationId,
  onDone,
}: {
  applicationId: string;
  onDone?: () => void;
}) {
  const copy = useCopy();
  const mutation = useCompleteCreditCheckAction(applicationId, onDone);
  const busy = mutation.isPending || mutation.isSuccess;
  const decide = (solvent: boolean) => mutation.mutate({ applicationId, data: { solvent } });
  return (
    <div className="flex gap-3">
      <Button onClick={() => decide(true)} disabled={busy}>
        {copy.creditCheck.solvent}
      </Button>
      <Button variant="danger" onClick={() => decide(false)} disabled={busy}>
        {copy.creditCheck.notSolvent}
      </Button>
    </div>
  );
}
