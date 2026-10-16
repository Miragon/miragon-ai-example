import { describe, expect, it } from "vitest";
import { HttpError } from "./http-client";
import { isConflict, isNotFound } from "./http-status";

describe("http status checks", () => {
  it("recognises a 409 problem as a conflict", () => {
    expect(isConflict(new HttpError(409, { detail: "stale" }))).toBe(true);
  });

  it("does not treat other statuses or plain errors as a conflict", () => {
    expect(isConflict(new HttpError(404, undefined))).toBe(false);
    expect(isConflict(new Error("409"))).toBe(false);
  });

  it("recognises a 404 problem as not found", () => {
    expect(isNotFound(new HttpError(404, undefined))).toBe(true);
  });

  it("does not treat other statuses or plain errors as not found", () => {
    expect(isNotFound(new HttpError(409, undefined))).toBe(false);
    expect(isNotFound(new Error("404"))).toBe(false);
  });
});
