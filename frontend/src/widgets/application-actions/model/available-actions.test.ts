import { describe, expect, it } from "vitest";
import { availableActions } from "./available-actions";

describe("availableActions", () => {
  it("offers only withdraw while the credit rating is checked", () => {
    expect(availableActions("RECEIVED")).toEqual(["withdraw"]);
  });

  it("offers sign and withdraw once the contract is sent", () => {
    expect(availableActions("CONTRACT_SENT")).toEqual(["sign", "withdraw"]);
  });

  it("offers handover and withdraw once ordered", () => {
    expect(availableActions("ORDERED")).toEqual(["handover", "withdraw"]);
  });

  it("offers nothing while the back office reverses a withdrawal", () => {
    expect(availableActions("WITHDRAWN")).toEqual([]);
  });

  it("offers nothing in the terminal statuses", () => {
    expect(availableActions("ACTIVE")).toEqual([]);
    expect(availableActions("REJECTED")).toEqual([]);
    expect(availableActions("CANCELLED")).toEqual([]);
  });

  it("offers nothing for an unknown status", () => {
    expect(availableActions("SOMETHING_NEW")).toEqual([]);
  });
});
