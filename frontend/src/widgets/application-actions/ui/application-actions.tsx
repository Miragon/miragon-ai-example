import {
  Alert,
  AlertDescription,
  AlertTitle,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/ui";
import { useCopy, type Copy } from "@/shared/i18n";
import type { LeasingStatus } from "@/entities/leasing-application";
import { SignContractButton } from "@/features/sign-contract";
import { ReportHandoverButton } from "@/features/report-handover";
import { WithdrawApplicationButton } from "@/features/withdraw-application";
import { availableActions } from "../model/available-actions";

function finishedNote(status: LeasingStatus | string, copy: Copy): string {
  if (status === "REJECTED") return copy.actions.terminalRejected;
  if (status === "CANCELLED") return copy.actions.terminalCancelled;
  return copy.actions.terminalActive;
}

export function ApplicationActions({
  applicationId,
  status,
}: {
  applicationId: string;
  status: LeasingStatus | string;
}) {
  const copy = useCopy();
  const actions = availableActions(status);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{copy.actions.title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div aria-live="polite" className="flex flex-col gap-4">
          {status === "WITHDRAWN" ? (
            <Alert variant="warning">
              <AlertTitle>{copy.actions.reversalPendingTitle}</AlertTitle>
              <AlertDescription>{copy.actions.reversalPendingNote}</AlertDescription>
            </Alert>
          ) : null}
          {status === "RECEIVED" ? (
            <p className="text-body text-schwarz/70">{copy.actions.creditCheckPendingNote}</p>
          ) : null}
          {actions.length > 0 ? (
            <div className="flex flex-wrap gap-3">
              {actions.includes("sign") ? (
                <SignContractButton applicationId={applicationId} />
              ) : null}
              {actions.includes("handover") ? (
                <ReportHandoverButton applicationId={applicationId} />
              ) : null}
              {actions.includes("withdraw") ? (
                <WithdrawApplicationButton applicationId={applicationId} />
              ) : null}
            </div>
          ) : status !== "WITHDRAWN" ? (
            <p className="text-body text-schwarz/70">{finishedNote(status, copy)}</p>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}
