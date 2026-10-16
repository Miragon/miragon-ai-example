import { expect, test } from "@playwright/test";
import { expectStatus, submitApplication, uniqueName } from "./helpers/ui";

// The form deliberately accepts an under-age applicant: "Validate application" is a process rule.
test("invalid application: an under-age applicant is rejected without a credit check", async ({
  page,
}) => {
  const name = uniqueName("Tim Toojung");
  await submitApplication(page, {
    name,
    email: "tim@example.com",
    age: 17,
    income: 3500,
    bikeModel: "Gravel Explorer 900",
  });

  await expectStatus(page, "abgelehnt");

  await page.goto("/aufgaben");
  await expect(page.getByRole("heading", { name: "aufgaben" })).toBeVisible();
  await expect(page.getByRole("row").filter({ hasText: name })).toHaveCount(0);
});
