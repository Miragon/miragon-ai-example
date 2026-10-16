import { expect, type Page } from "@playwright/test";

export interface ApplicationInput {
  name: string;
  email: string;
  age: number;
  income: number;
  bikeModel: string;
}

/** A customer name unique per run, so a row in the shared inbox or list belongs to this test. */
export function uniqueName(name: string): string {
  return `${name} ${Date.now().toString(36)}`;
}

export async function submitApplication(page: Page, input: ApplicationInput): Promise<string> {
  await page.goto("/antraege/neu");
  await page.getByLabel(/^name/i).fill(input.name);
  await page.getByLabel(/^e-mail/i).fill(input.email);
  await page.getByLabel(/^alter/i).fill(String(input.age));
  await page.getByLabel(/nettoeinkommen/i).fill(String(input.income));

  await page.getByLabel(/^fahrrad/i).click();
  await page.getByRole("option", { name: input.bikeModel }).click();

  await page.getByRole("button", { name: /antrag absenden/i }).click();

  await page.waitForURL(/\/antraege\/[0-9a-f-]{36}$/i, { timeout: 15_000 });
  const match = page.url().match(/\/antraege\/([0-9a-f-]{36})/i);
  expect(match, "expected to land on the detail page").not.toBeNull();
  return match![1];
}

export async function openApplication(page: Page, applicationId: string): Promise<void> {
  await page.goto(`/antraege/${applicationId}`);
}

/** Waits for the detail page's status badge (not the progress rail) to show the German label. */
export async function expectStatus(page: Page, label: string): Promise<void> {
  await expect(page.getByTestId("application-status")).toHaveText(label, { timeout: 20_000 });
}

/** Opens the inbox task of the given type for the customer; the inbox polls until it appears. */
export async function openInboxTask(
  page: Page,
  customerName: string,
  taskType: "bonitätsprüfung" | "rückabwicklung",
): Promise<void> {
  await page.goto("/aufgaben");
  const row = page.getByRole("row").filter({ hasText: customerName }).filter({ hasText: taskType });
  await row.getByRole("button", { name: `bearbeiten: ${customerName}` }).click({ timeout: 20_000 });
  await expect(page.getByRole("dialog")).toBeVisible();
}

export async function decideCreditCheck(
  page: Page,
  customerName: string,
  decision: "solvent" | "nicht solvent",
): Promise<void> {
  await openInboxTask(page, customerName, "bonitätsprüfung");
  await page.getByRole("dialog").getByRole("button", { name: decision, exact: true }).click();
  await expect(page.getByRole("dialog")).toBeHidden();
}
