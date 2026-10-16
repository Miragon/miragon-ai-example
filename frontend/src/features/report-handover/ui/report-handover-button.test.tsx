import { describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server, waitFor } from "@/shared/testing";
import { Toaster } from "@/shared/ui";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";
import { ReportHandoverButton } from "./report-handover-button";

describe("ReportHandoverButton", () => {
  it("stays disabled after a successful report so a second click can't correlate twice", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    let posts = 0;
    server.use(
      http.post("*/api/bike-leasing/:id/report-handover", () => {
        posts += 1;
        return new HttpResponse(null, { status: 202 });
      }),
    );

    renderWithProviders(<ReportHandoverButton applicationId="app-1" />);
    const button = screen.getByRole("button");

    await user.click(button);
    await waitFor(() => expect(button).toBeDisabled());

    await user.click(button);
    expect(posts).toBe(1);
  });

  it("on a 409 says the application changed and re-reads it", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.post("*/api/bike-leasing/:id/report-handover", () =>
        HttpResponse.json({ status: 409, detail: "raw engine detail" }, { status: 409 }),
      ),
    );
    const { queryClient } = renderWithProviders(
      <>
        <ReportHandoverButton applicationId="app-1" />
        <Toaster />
      </>,
    );
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await user.click(screen.getByRole("button", { name: "übergabe melden" }));

    expect(await screen.findByText("der antrag hat sich inzwischen geändert.")).toBeInTheDocument();
    expect(screen.queryByText("raw engine detail")).not.toBeInTheDocument();
    expect(invalidate.mock.calls.map(([filters]) => filters?.queryKey)).toEqual(
      expect.arrayContaining([
        leasingApplicationKeys.all,
        leasingApplicationKeys.detail("app-1"),
        openTaskKeys.all,
      ]),
    );
  });
});
