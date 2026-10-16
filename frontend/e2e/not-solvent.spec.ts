import { test } from "@playwright/test";
import {
  decideCreditCheck,
  expectStatus,
  openApplication,
  submitApplication,
  uniqueName,
} from "./helpers/ui";

test("not solvent: the back office rejects the credit rating and the application is rejected", async ({
  page,
}) => {
  const name = uniqueName("Nora Notsolvent");
  const applicationId = await submitApplication(page, {
    name,
    email: "nora@example.com",
    age: 40,
    income: 900,
    bikeModel: "Carbon Road 800",
  });

  await decideCreditCheck(page, name, "nicht solvent");

  await openApplication(page, applicationId);
  await expectStatus(page, "abgelehnt");
});
