import { describe, expect, it } from "vitest";
import { de, en } from "@/shared/i18n";
import { isTerminal, LEASING_STATUSES, statusLabel, statusTone } from "./status";

describe("leasing status model", () => {
  it("treats ACTIVE, REJECTED and CANCELLED as terminal", () => {
    expect(isTerminal("ACTIVE")).toBe(true);
    expect(isTerminal("REJECTED")).toBe(true);
    expect(isTerminal("CANCELLED")).toBe(true);
  });

  it("treats every status the engine still works on as non-terminal", () => {
    expect(isTerminal("RECEIVED")).toBe(false);
    expect(isTerminal("CONTRACT_SENT")).toBe(false);
    expect(isTerminal("ORDERED")).toBe(false);
    expect(isTerminal("WITHDRAWN")).toBe(false);
  });

  it("treats a missing status as non-terminal", () => {
    expect(isTerminal(undefined)).toBe(false);
    expect(isTerminal(null)).toBe(false);
  });

  it("has a label in every locale and a badge tone for every status", () => {
    for (const status of LEASING_STATUSES) {
      expect(statusLabel(status, de)).toMatch(/\p{L}/u);
      expect(statusLabel(status, en)).toMatch(/\p{L}/u);
      expect(statusTone(status)).not.toBe("neutral");
    }
  });

  it("maps active to success, rejected to danger and withdrawn to warning", () => {
    expect(statusTone("ACTIVE")).toBe("success");
    expect(statusTone("REJECTED")).toBe("danger");
    expect(statusTone("WITHDRAWN")).toBe("warning");
  });

  it("falls back to the raw value and a neutral tone for an unknown status", () => {
    expect(statusLabel("SOMETHING_NEW", de)).toBe("SOMETHING_NEW");
    expect(statusTone("SOMETHING_NEW")).toBe("neutral");
  });
});
