import { describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server, waitFor } from "@/shared/testing";
import { SubmitApplicationPage } from "./submit-application-page";

describe("SubmitApplicationPage", () => {
  it("posts exactly the entered application, including an under-age applicant", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    let captured: unknown;
    server.use(
      http.get("*/api/bikes", () =>
        HttpResponse.json([
          { bikeId: "BIKE-900", model: "Gravel Explorer 900" },
          { bikeId: "BIKE-600", model: "Mountain Trail 600" },
        ]),
      ),
      http.post("*/api/bike-leasing", async ({ request }) => {
        captured = await request.json();
        return HttpResponse.json({ applicationId: "new-id" });
      }),
    );

    const onCreated = vi.fn();
    renderWithProviders(<SubmitApplicationPage onCreated={onCreated} />);

    await user.type(screen.getByLabelText(/name/i), "Alice Rider");
    await user.type(screen.getByLabelText(/e-mail/i), "alice@example.com");
    await user.type(screen.getByLabelText(/alter/i), "17");
    await user.type(screen.getByLabelText(/einkommen/i), "3500");

    const picker = screen.getByLabelText(/fahrrad/i);
    expect(picker).toHaveRole("combobox");
    await user.click(picker);
    await user.click(await screen.findByRole("option", { name: "Gravel Explorer 900" }));

    await user.click(screen.getByRole("button", { name: /antrag absenden/i }));

    await waitFor(() => expect(captured).toBeTruthy());
    expect(captured).toEqual({
      customerName: "Alice Rider",
      email: "alice@example.com",
      age: 17,
      monthlyNetIncome: 3500,
      bikeId: "BIKE-900",
    });
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith("new-id"));
  });
});
