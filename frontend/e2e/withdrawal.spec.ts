import { test } from "@playwright/test";
import {
  expectStatus,
  openApplication,
  openInboxTask,
  submitApplication,
  uniqueName,
} from "./helpers/ui";

test("withdrawal: a withdrawn application is reversed by the back office and cancelled", async ({
  page,
}) => {
  const name = uniqueName("Wanda Withdraw");
  const applicationId = await submitApplication(page, {
    name,
    email: "wanda@example.com",
    age: 33,
    income: 3200,
    bikeModel: "Mountain Trail 600",
  });
  await expectStatus(page, "eingegangen");

  await page.getByRole("button", { name: /antrag zurückziehen/i }).click();
  await expectStatus(page, "zurückgezogen");

  await openInboxTask(page, name, "rückabwicklung");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: /rückabwicklung bestätigen/i })
    .click();

  await openApplication(page, applicationId);
  await expectStatus(page, "storniert");
});
