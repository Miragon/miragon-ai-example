import { useId, type KeyboardEvent } from "react";
import {
  Button,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
} from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { formatDateTime } from "@/shared/lib";
import {
  LEASING_STATUSES,
  StatusBadge,
  statusLabel,
  type LeasingApplicationDto,
  type LeasingStatus,
} from "@/entities/leasing-application";

const ALL = "ALL";

function isActivationKey(event: KeyboardEvent) {
  return event.key === "Enter" || event.key === " ";
}

function Pager({
  pageNumber,
  totalPages,
  onPageChange,
}: {
  pageNumber: number;
  totalPages: number;
  onPageChange: (pageNumber: number) => void;
}) {
  const copy = useCopy();
  return (
    <div className="flex items-center justify-end gap-3">
      <Button
        size="sm"
        variant="secondary"
        disabled={pageNumber <= 1}
        onClick={() => onPageChange(pageNumber - 1)}
      >
        {copy.list.previousPage}
      </Button>
      <span className="text-klein text-schwarz/60">
        {`${copy.list.page} ${pageNumber} ${copy.list.pageOf} ${totalPages}`}
      </span>
      <Button
        size="sm"
        variant="secondary"
        disabled={pageNumber >= totalPages}
        onClick={() => onPageChange(pageNumber + 1)}
      >
        {copy.list.nextPage}
      </Button>
    </div>
  );
}

export function ApplicationTable({
  items,
  status,
  onStatusChange,
  onSelect,
  pageNumber,
  totalPages,
  onPageChange,
}: {
  items: LeasingApplicationDto[];
  status: LeasingStatus | null;
  onStatusChange: (status: LeasingStatus | null) => void;
  onSelect: (applicationId: string) => void;
  pageNumber: number;
  totalPages: number;
  onPageChange: (pageNumber: number) => void;
}) {
  const copy = useCopy();
  const statusFilterId = useId();
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <label htmlFor={statusFilterId} className="text-klein text-schwarz/60">
          {copy.list.filterLabel}
        </label>
        <Select
          value={status ?? ALL}
          onValueChange={(value) => onStatusChange(value === ALL ? null : (value as LeasingStatus))}
        >
          <SelectTrigger id={statusFilterId} className="w-56">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{copy.list.filterAll}</SelectItem>
            {LEASING_STATUSES.map((s) => (
              <SelectItem key={s} value={s}>
                {statusLabel(s, copy)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Table>
        <THead>
          <TR>
            <TH>{copy.list.columnCustomer}</TH>
            <TH>{copy.list.columnBike}</TH>
            <TH>{copy.list.columnStatus}</TH>
            <TH>{copy.list.columnCreated}</TH>
          </TR>
        </THead>
        <TBody>
          {items.map((item) => (
            <TR
              key={item.applicationId}
              tabIndex={0}
              className="cursor-pointer hover:bg-grau focus-visible:bg-grau focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-blau"
              onClick={() => onSelect(item.applicationId)}
              onKeyDown={(event) => {
                if (!isActivationKey(event)) return;
                event.preventDefault();
                onSelect(item.applicationId);
              }}
            >
              <TD>{item.customerName}</TD>
              <TD>{item.bikeModel ?? item.bikeId}</TD>
              <TD>
                <StatusBadge status={item.status} />
              </TD>
              <TD>{formatDateTime(item.createdAt)}</TD>
            </TR>
          ))}
        </TBody>
      </Table>

      {totalPages > 1 ? (
        <Pager pageNumber={pageNumber} totalPages={totalPages} onPageChange={onPageChange} />
      ) : null}
    </div>
  );
}
