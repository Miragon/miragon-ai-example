import { describe, expect, it } from "vitest";
import { de } from "@/shared/i18n";
import { makeSubmitFormSchema } from "./submit-form-schema";

const request = {
  customerName: "Alice Rider",
  email: "alice@example.com",
  age: 35,
  monthlyNetIncome: 3500,
  bikeId: "BIKE-900",
};

describe("submit form schema", () => {
  const schema = makeSubmitFormSchema(de);

  it("accepts a complete request", () => {
    expect(schema.safeParse(request).success).toBe(true);
  });

  it("accepts an under-age applicant, because validating the age is the process's job", () => {
    expect(schema.safeParse({ ...request, age: 17 }).success).toBe(true);
  });

  it("accepts a bike id outside the catalogue, because the process validates it", () => {
    expect(schema.safeParse({ ...request, bikeId: "BIKE-UNKNOWN" }).success).toBe(true);
  });

  it("refuses an income of zero with its own message", () => {
    const result = schema.safeParse({ ...request, monthlyNetIncome: 0 });

    expect(result.success).toBe(false);
    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      de.submit.validationIncomePositive,
    ]);
  });

  it("names every missing field", () => {
    const result = schema.safeParse({ customerName: "", email: "", bikeId: "" });

    expect(result.success).toBe(false);
    expect([...new Set(result.error?.issues.map((issue) => issue.message))].sort()).toEqual(
      [
        de.submit.validationName,
        de.submit.validationEmail,
        de.submit.validationAge,
        de.submit.validationIncome,
        de.submit.validationBike,
      ].sort(),
    );
  });
});
