import { Check, Circle, Clock, X } from "lucide-react";
import { cn } from "@/shared/lib";
import { useCopy, type Copy } from "@/shared/i18n";
import type { LeasingStatus } from "@/entities/leasing-application";

/**
 * The step rail of the BPMN happy path with abgelehnt / zurückgezogen / storniert as off-ramps.
 * A status is a milestone already reached, so its step is done and the next one is in progress.
 */
const MAIN_PATH: readonly LeasingStatus[] = [
  "RECEIVED",
  "CONTRACT_SENT",
  "CONTRACT_SIGNED",
  "ORDERED",
  "ACTIVE",
];

function mainStepLabel(status: LeasingStatus, copy: Copy): string {
  const labels: Partial<Record<LeasingStatus, string>> = {
    RECEIVED: copy.progress.received,
    CONTRACT_SENT: copy.progress.contractSent,
    CONTRACT_SIGNED: copy.progress.contractSigned,
    ORDERED: copy.progress.ordered,
    ACTIVE: copy.progress.active,
  };
  return labels[status] ?? status;
}

function offRampLabel(status: LeasingStatus | string, copy: Copy): string {
  if (status === "WITHDRAWN") return copy.progress.withdrawn;
  if (status === "REJECTED") return copy.progress.rejected;
  return copy.progress.cancelled;
}

export function LeasingProgress({ status }: { status: LeasingStatus | string }) {
  const copy = useCopy();
  const reachedIndex = MAIN_PATH.indexOf(status as LeasingStatus);
  const isOffRamp = reachedIndex === -1;
  const awaitsReversal = status === "WITHDRAWN";

  return (
    <ol className="flex flex-col gap-3" aria-label={copy.progress.title}>
      {MAIN_PATH.map((step, index) => {
        const done = !isOffRamp && index <= reachedIndex;
        const inProgress = !isOffRamp && index === reachedIndex + 1;
        return (
          <li key={step} className="flex items-center gap-3">
            <span
              className={cn(
                "flex size-6 items-center justify-center rounded-pill",
                done && "bg-success text-weiss",
                inProgress && "bg-blau text-weiss",
                !done && !inProgress && "bg-grau text-schwarz/40",
              )}
            >
              {done ? (
                <Check size={16} strokeWidth={1.75} />
              ) : (
                <Circle size={16} strokeWidth={1.75} />
              )}
            </span>
            <span
              className={cn(
                "text-body",
                inProgress ? "font-medium text-schwarz" : "text-schwarz/60",
              )}
            >
              {mainStepLabel(step, copy)}
            </span>
          </li>
        );
      })}
      {isOffRamp ? (
        <li className="flex items-center gap-3">
          <span
            className={cn(
              "flex size-6 items-center justify-center rounded-pill text-weiss",
              awaitsReversal ? "bg-warning" : "bg-danger",
            )}
          >
            {awaitsReversal ? (
              <Clock size={16} strokeWidth={1.75} />
            ) : (
              <X size={16} strokeWidth={1.75} />
            )}
          </span>
          <span
            className={cn("text-body font-medium", awaitsReversal ? "text-warning" : "text-danger")}
          >
            {offRampLabel(status, copy)}
          </span>
        </li>
      ) : null}
    </ol>
  );
}
