import { describe, expect, it } from "vitest";
import { renderWithProviders, screen } from "@/shared/testing";
import { ApplicationActions } from "./application-actions";

const buttonNames = () => screen.queryAllByRole("button").map((button) => button.textContent);

describe("ApplicationActions", () => {
  it("explains the credit check and offers only the withdrawal while RECEIVED", () => {
    renderWithProviders(<ApplicationActions applicationId="app-1" status="RECEIVED" />);
    expect(screen.getByText("das back office prüft gerade die bonität.")).toBeInTheDocument();
    expect(buttonNames()).toEqual(["antrag zurückziehen"]);
  });

  it("offers signing and withdrawing once the contract is sent", () => {
    renderWithProviders(<ApplicationActions applicationId="app-1" status="CONTRACT_SENT" />);
    expect(buttonNames()).toEqual(["vertrag unterschreiben", "antrag zurückziehen"]);
  });

  it("points at the back-office reversal and offers no action while WITHDRAWN", () => {
    renderWithProviders(<ApplicationActions applicationId="app-1" status="WITHDRAWN" />);
    expect(screen.getByRole("alert")).toHaveTextContent("antrag zurückgezogen");
    expect(buttonNames()).toEqual([]);
  });

  it("explains why a rejected case offers no action", () => {
    renderWithProviders(<ApplicationActions applicationId="app-1" status="REJECTED" />);
    expect(
      screen.getByText("der antrag wurde abgelehnt. es sind keine aktionen möglich."),
    ).toBeInTheDocument();
    expect(buttonNames()).toEqual([]);
  });
});
