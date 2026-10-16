import { describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server, waitFor } from "@/shared/testing";
import { Toaster } from "@/shared/ui";
import { leasingApplicationKeys } from "@/entities/leasing-application";
import { openTaskKeys } from "@/entities/user-task";
import { SignContractButton } from "./sign-contract-button";

function renderButton() {
  return renderWithProviders(
    <>
      <SignContractButton applicationId="app-1" />
      <Toaster />
    </>,
  );
}

describe("SignContractButton", () => {
  it("on a 409 says the application changed and re-reads it instead of showing the raw detail", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.post("*/api/bike-leasing/:id/sign-contract", () =>
        HttpResponse.json({ status: 409, detail: "raw engine detail" }, { status: 409 }),
      ),
    );
    const { queryClient } = renderButton();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await user.click(screen.getByRole("button", { name: "vertrag unterschreiben" }));

    expect(await screen.findByText("der antrag hat sich inzwischen geändert.")).toBeInTheDocument();
    expect(screen.queryByText("raw engine detail")).not.toBeInTheDocument();
    const invalidatedKeys = invalidate.mock.calls.map(([filters]) => filters?.queryKey);
    expect(invalidatedKeys).toEqual(
      expect.arrayContaining([
        leasingApplicationKeys.all,
        leasingApplicationKeys.detail("app-1"),
        openTaskKeys.all,
      ]),
    );
  });

  it("keeps the generic error toast for any other failure", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.post("*/api/bike-leasing/:id/sign-contract", () =>
        HttpResponse.json({ status: 500 }, { status: 500 }),
      ),
    );
    const { queryClient } = renderButton();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await user.click(screen.getByRole("button", { name: "vertrag unterschreiben" }));

    expect(
      await screen.findByText("die aktion konnte nicht ausgeführt werden."),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "vertrag unterschreiben" })).toBeEnabled(),
    );
    expect(screen.queryByText("der antrag hat sich inzwischen geändert.")).not.toBeInTheDocument();
    expect(invalidate).not.toHaveBeenCalled();
  });
});
