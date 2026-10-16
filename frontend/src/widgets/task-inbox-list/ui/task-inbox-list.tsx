import { useState, type ReactNode } from "react";
import {
  Badge,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  Table,
  TBody,
  TD,
  TH,
  THead,
  TR,
} from "@/shared/ui";
import { useCopy } from "@/shared/i18n";
import { formatDateTime, formatEuro } from "@/shared/lib";
import type { OpenTaskDto } from "@/entities/user-task";
import { CreditRatingDecision } from "@/features/check-credit-rating";
import { ConfirmReversalButton } from "@/features/reverse-application";

function Fact({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex justify-between gap-4 border-b border-schwarz/5 py-2 last:border-0">
      <dt className="text-klein text-schwarz/60">{label}</dt>
      <dd className="text-body text-schwarz">{value}</dd>
    </div>
  );
}

function CreditCheckDialogBody({ task, onDone }: { task: OpenTaskDto; onDone: () => void }) {
  const copy = useCopy();
  const { application } = task;
  return (
    <>
      <DialogHeader>
        <DialogTitle>{copy.creditCheck.title}</DialogTitle>
        <DialogDescription>{copy.creditCheck.subtitle}</DialogDescription>
      </DialogHeader>
      <dl>
        <Fact label={copy.detail.customer} value={application.customerName} />
        <Fact label={copy.detail.age} value={`${application.age} ${copy.detail.years}`} />
        <Fact label={copy.detail.income} value={formatEuro(application.monthlyNetIncome)} />
        <Fact label={copy.detail.bike} value={application.bikeModel ?? application.bikeId} />
      </dl>
      <CreditRatingDecision applicationId={application.applicationId} onCompleted={onDone} />
    </>
  );
}

function ReversalDialogBody({ task, onDone }: { task: OpenTaskDto; onDone: () => void }) {
  const copy = useCopy();
  return (
    <>
      <DialogHeader>
        <DialogTitle>{copy.reversal.title}</DialogTitle>
        <DialogDescription>{copy.reversal.subtitle}</DialogDescription>
      </DialogHeader>
      <div>
        <ConfirmReversalButton
          applicationId={task.application.applicationId}
          onCompleted={onDone}
        />
      </div>
    </>
  );
}

export function TaskInboxList({ tasks }: { tasks: OpenTaskDto[] }) {
  const copy = useCopy();
  const [openTask, setOpenTask] = useState<OpenTaskDto | null>(null);
  const close = () => setOpenTask(null);

  return (
    <>
      <Table>
        <THead>
          <TR>
            <TH>{copy.inbox.columnType}</TH>
            <TH>{copy.inbox.columnCustomer}</TH>
            <TH>{copy.inbox.columnBike}</TH>
            <TH>{copy.inbox.columnWaitingSince}</TH>
            <TH aria-label={copy.inbox.open} />
          </TR>
        </THead>
        <TBody>
          {tasks.map((task) => (
            <TR key={`${task.type}-${task.application.applicationId}`}>
              <TD>
                <Badge variant={task.type === "CREDIT_CHECK" ? "info" : "warning"}>
                  {copy.inbox.taskType[task.type]}
                </Badge>
              </TD>
              <TD>{task.application.customerName}</TD>
              <TD>{task.application.bikeModel ?? task.application.bikeId}</TD>
              <TD>{formatDateTime(task.waitingSince)}</TD>
              <TD>
                <Button size="sm" variant="secondary" onClick={() => setOpenTask(task)}>
                  {copy.inbox.open}
                </Button>
              </TD>
            </TR>
          ))}
        </TBody>
      </Table>

      <Dialog open={openTask !== null} onOpenChange={(open) => (open ? null : close())}>
        <DialogContent>
          {openTask?.type === "CREDIT_CHECK" ? (
            <CreditCheckDialogBody task={openTask} onDone={close} />
          ) : null}
          {openTask?.type === "REVERSAL" ? (
            <ReversalDialogBody task={openTask} onDone={close} />
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
