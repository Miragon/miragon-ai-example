import { describe, expect, it } from "vitest";
import userEvent from "@testing-library/user-event";
import { renderWithProviders, screen, waitFor, within } from "@/shared/testing";
import type { OpenTaskDto } from "@/entities/user-task";
import { TaskInboxList } from "./task-inbox-list";

function creditCheckTask(applicationId: string, customerName: string, age = 35): OpenTaskDto {
  return {
    type: "CREDIT_CHECK",
    waitingSince: "2026-08-17T10:31:00Z",
    application: {
      applicationId,
      customerName,
      email: "someone@example.com",
      age,
      monthlyNetIncome: 3500,
      bikeId: "BIKE-900",
      bikeModel: "Gravel Explorer 900",
      status: "RECEIVED",
      contractId: null,
      orderId: null,
      policyId: null,
      createdAt: "2026-08-17T10:30:00Z",
    },
  };
}

describe("TaskInboxList", () => {
  it("names every row's button after its customer", () => {
    renderWithProviders(
      <TaskInboxList
        tasks={[creditCheckTask("app-1", "Alice Rider"), creditCheckTask("app-2", "Bob Biker")]}
      />,
    );

    expect(screen.getByRole("button", { name: "bearbeiten: Alice Rider" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "bearbeiten: Bob Biker" })).toBeInTheDocument();
  });

  it("shows the latest polled task in the open dialog, not a snapshot", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    const { rerender } = renderWithProviders(
      <TaskInboxList tasks={[creditCheckTask("app-1", "Alice Rider", 35)]} />,
    );
    await user.click(screen.getByRole("button", { name: "bearbeiten: Alice Rider" }));
    expect(within(await screen.findByRole("dialog")).getByText("35 jahre")).toBeInTheDocument();

    rerender(<TaskInboxList tasks={[creditCheckTask("app-1", "Alice Rider", 36)]} />);

    expect(within(screen.getByRole("dialog")).getByText("36 jahre")).toBeInTheDocument();
  });

  it("closes the dialog once the polled inbox no longer has the task", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    const { rerender } = renderWithProviders(
      <TaskInboxList
        tasks={[creditCheckTask("app-1", "Alice Rider"), creditCheckTask("app-2", "Bob Biker")]}
      />,
    );
    await user.click(screen.getByRole("button", { name: "bearbeiten: Alice Rider" }));
    await screen.findByRole("dialog");

    rerender(<TaskInboxList tasks={[creditCheckTask("app-2", "Bob Biker")]} />);

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});
