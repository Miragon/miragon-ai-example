import { test } from "@playwright/test";
import {
  decideCreditCheck,
  expectStatus,
  openApplication,
  submitApplication,
  uniqueName,
} from "./helpers/ui";

test("happy path: a solvent customer signs, gets the bike handed over and the leasing goes active", async ({
  page,
}) => {
  const name = uniqueName("Alice Rider");
  const applicationId = await submitApplication(page, {
    name,
    email: "alice@example.com",
    age: 35,
    income: 3500,
    bikeModel: "Gravel Explorer 900",
  });
  await expectStatus(page, "eingegangen");

  await decideCreditCheck(page, name, "solvent");

  await openApplication(page, applicationId);
  await expectStatus(page, "vertrag versendet");
  await page.getByRole("button", { name: /vertrag unterschreiben/i }).click();

  await expectStatus(page, "bestellt");
  await page.getByRole("button", { name: /übergabe melden/i }).click();

  await expectStatus(page, "aktiv");
});
