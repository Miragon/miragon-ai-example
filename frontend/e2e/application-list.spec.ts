import { expect, test } from "@playwright/test";
import { expectStatus, submitApplication, uniqueName } from "./helpers/ui";

test("application list: a new application shows up under its status and opens from the keyboard", async ({
  page,
}) => {
  const name = uniqueName("Lena Liste");
  const applicationId = await submitApplication(page, {
    name,
    email: "lena@example.com",
    age: 31,
    income: 3100,
    bikeModel: "Gravel Explorer 900",
  });
  await expectStatus(page, "eingegangen");

  await page.goto("/");
  await page.getByLabel("status", { exact: true }).click();
  await page.getByRole("option", { name: "eingegangen" }).click();
  await expect(page).toHaveURL(/status=RECEIVED/);

  const row = page.getByRole("row").filter({ hasText: name });
  await expect(row).toContainText("eingegangen");

  await row.focus();
  await page.keyboard.press("Enter");

  await expect(page).toHaveURL(new RegExp(`/antraege/${applicationId}$`));
  await expectStatus(page, "eingegangen");
});
