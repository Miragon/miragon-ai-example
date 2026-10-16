import { describe, expect, it } from "vitest";
import { renderWithProviders, screen } from "@/shared/testing";
import { LeasingProgress } from "./leasing-progress";

const circleOf = (label: string) => screen.getByText(label).closest("li")!.querySelector("span")!;

describe("LeasingProgress", () => {
  it("marks the reached step done and the next step in progress while CONTRACT_SENT", () => {
    renderWithProviders(<LeasingProgress status="CONTRACT_SENT" />);
    expect(circleOf("eingegangen").className).toContain("bg-success");
    expect(circleOf("vertrag versendet").className).toContain("bg-success");
    expect(screen.getByText("vertrag versendet").className).not.toContain("font-medium");
    expect(screen.getByText("bestellt").className).toContain("font-medium");
    expect(circleOf("aktiv").className).toContain("bg-grau");
  });

  it("marks every step done once ACTIVE", () => {
    renderWithProviders(<LeasingProgress status="ACTIVE" />);
    for (const label of ["eingegangen", "vertrag versendet", "bestellt", "aktiv"]) {
      expect(circleOf(label).className).toContain("bg-success");
    }
  });

  it("shows the withdrawal as an off-ramp in progress, not as a failure", () => {
    renderWithProviders(<LeasingProgress status="WITHDRAWN" />);
    const circle = circleOf("zurückgezogen");
    expect(circle.className).toContain("bg-warning");
    expect(circle.className).not.toContain("bg-danger");
  });

  it("shows a rejection as a failed off-ramp with no step done", () => {
    renderWithProviders(<LeasingProgress status="REJECTED" />);
    expect(circleOf("abgelehnt").className).toContain("bg-danger");
    expect(circleOf("eingegangen").className).not.toContain("bg-success");
  });
});
