import { Badge } from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { statusLabel, statusTone, type LeasingStatus } from "../model/status";

export function StatusBadge({ status }: { status: LeasingStatus | string }) {
  const copy = useCopy();
  return <Badge variant={statusTone(status)}>{statusLabel(status, copy)}</Badge>;
}
