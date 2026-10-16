import { Button } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { useSignContractAction } from "../api/use-sign-contract";

export function SignContractButton({ applicationId }: { applicationId: string }) {
  const copy = useCopy();
  const mutation = useSignContractAction(applicationId);
  const busy = mutation.isPending || mutation.isSuccess;
  return (
    <Button onClick={() => mutation.mutate({ applicationId })} disabled={busy}>
      {busy ? copy.actions.signing : copy.actions.sign}
    </Button>
  );
}
