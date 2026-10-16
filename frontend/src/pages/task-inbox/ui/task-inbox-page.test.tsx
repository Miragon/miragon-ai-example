import { describe, expect, it } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server, waitFor, within } from "@/shared/testing";
import { Toaster } from "@/shared/ui";
import { TaskInboxPage } from "./task-inbox-page";

function application(applicationId: string, customerName: string, status: string) {
  return {
    applicationId,
    customerName,
    email: "someone@example.com",
    age: 35,
    monthlyNetIncome: 3500,
    bikeId: "BIKE-900",
    bikeModel: "Gravel Explorer 900",
    status,
    contractId: null,
    orderId: null,
    policyId: null,
    createdAt: "2026-08-17T10:30:00Z",
  };
}

const openTasks = [
  {
    type: "CREDIT_CHECK",
    waitingSince: "2026-08-17T10:31:00Z",
    application: application("app-credit", "Alice Rider", "RECEIVED"),
  },
  {
    type: "REVERSAL",
    waitingSince: "2026-08-17T10:32:00Z",
    application: application("app-reversal", "Wanda Withdraw", "WITHDRAWN"),
  },
];

function rowOf(customerName: string) {
  return screen.getByText(customerName).closest("tr")!;
}

describe("TaskInboxPage", () => {
  it("lists every open task with its type", async () => {
    server.use(http.get("*/api/tasks", () => HttpResponse.json(openTasks)));
    renderWithProviders(<TaskInboxPage />);

    expect(await screen.findByText("Alice Rider")).toBeInTheDocument();
    expect(within(rowOf("Alice Rider")).getByText("bonitätsprüfung")).toBeInTheDocument();
    expect(within(rowOf("Wanda Withdraw")).getByText("rückabwicklung")).toBeInTheDocument();
  });

  it("shows the empty state when no task is open", async () => {
    server.use(http.get("*/api/tasks", () => HttpResponse.json([])));
    renderWithProviders(<TaskInboxPage />);
    expect(await screen.findByText("keine offenen aufgaben.")).toBeInTheDocument();
  });

  it("completes a credit check as not solvent and closes the dialog", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    let decision: unknown;
    let decidedFor: unknown;
    server.use(
      http.get("*/api/tasks", () => HttpResponse.json(openTasks)),
      http.post("*/api/bike-leasing/:id/credit-check", async ({ params, request }) => {
        decidedFor = params.id;
        decision = await request.json();
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderWithProviders(<TaskInboxPage />);

    await screen.findByText("Alice Rider");
    await user.click(screen.getByRole("button", { name: "bearbeiten: Alice Rider" }));
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByText("35 jahre")).toBeInTheDocument();
    expect(within(dialog).getByText("Gravel Explorer 900")).toBeInTheDocument();

    await user.click(within(dialog).getByRole("button", { name: "nicht solvent" }));

    await waitFor(() => expect(decision).toEqual({ solvent: false }));
    expect(decidedFor).toBe("app-credit");
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("confirms a reversal for the withdrawn application", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    let reversedFor: unknown;
    server.use(
      http.get("*/api/tasks", () => HttpResponse.json(openTasks)),
      http.post("*/api/bike-leasing/:id/reversal", ({ params }) => {
        reversedFor = params.id;
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderWithProviders(<TaskInboxPage />);

    await screen.findByText("Wanda Withdraw");
    await user.click(screen.getByRole("button", { name: "bearbeiten: Wanda Withdraw" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "rückabwicklung bestätigen" }));

    await waitFor(() => expect(reversedFor).toBe("app-reversal"));
  });

  it("closes the dialog and says the application changed when the task is already gone (409)", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.get("*/api/tasks", () => HttpResponse.json(openTasks)),
      http.post("*/api/bike-leasing/:id/credit-check", () =>
        HttpResponse.json({ status: 409, detail: "raw engine detail" }, { status: 409 }),
      ),
    );
    renderWithProviders(
      <>
        <TaskInboxPage />
        <Toaster />
      </>,
    );

    await user.click(await screen.findByRole("button", { name: "bearbeiten: Alice Rider" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "solvent" }));

    expect(await screen.findByText("der antrag hat sich inzwischen geändert.")).toBeInTheDocument();
    expect(screen.queryByText("raw engine detail")).not.toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("closes the reversal dialog on a 409 as well", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.get("*/api/tasks", () => HttpResponse.json(openTasks)),
      http.post("*/api/bike-leasing/:id/reversal", () =>
        HttpResponse.json({ status: 409 }, { status: 409 }),
      ),
    );
    renderWithProviders(
      <>
        <TaskInboxPage />
        <Toaster />
      </>,
    );

    await user.click(await screen.findByRole("button", { name: "bearbeiten: Wanda Withdraw" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "rückabwicklung bestätigen" }));

    expect(await screen.findByText("der antrag hat sich inzwischen geändert.")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});
