import { describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server } from "@/shared/testing";
import { Toaster } from "@/shared/ui";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";
import { WithdrawApplicationButton } from "./withdraw-application-button";

describe("WithdrawApplicationButton", () => {
  it("on a 409 says the application changed and re-reads it", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.post("*/api/bike-leasing/:id/withdraw", () =>
        HttpResponse.json({ status: 409, detail: "raw engine detail" }, { status: 409 }),
      ),
    );
    const { queryClient } = renderWithProviders(
      <>
        <WithdrawApplicationButton applicationId="app-1" />
        <Toaster />
      </>,
    );
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await user.click(screen.getByRole("button", { name: "antrag zurückziehen" }));

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
