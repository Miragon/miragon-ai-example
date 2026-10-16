import { afterEach, describe, expect, it, vi } from "vitest";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server } from "@/shared/testing";
import { ApplicationListPage } from "./application-list-page";

const noop = () => {};

function page(overrides: Partial<Parameters<typeof ApplicationListPage>[0]> = {}) {
  return (
    <ApplicationListPage
      status={null}
      onStatusChange={noop}
      pageNumber={1}
      onPageChange={noop}
      onSelect={noop}
      onNew={noop}
      {...overrides}
    />
  );
}

const oneApplication = {
  items: [
    {
      applicationId: "app-1",
      customerName: "Alice Rider",
      email: "alice@example.com",
      age: 35,
      monthlyNetIncome: 3500,
      bikeId: "BIKE-900",
      bikeModel: "Gravel Explorer 900",
      status: "CONTRACT_SENT",
      contractId: "CONTRACT-1",
      orderId: null,
      policyId: null,
      createdAt: "2026-08-17T10:30:00Z",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

function pageOf(pageIndex: number, totalPages: number) {
  return { ...oneApplication, page: pageIndex, totalElements: totalPages * 20, totalPages };
}

describe("ApplicationListPage", () => {
  afterEach(() => vi.useRealTimers());

  it("renders the rows once the applications load", async () => {
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(oneApplication)));
    renderWithProviders(page());
    expect(await screen.findByText("Alice Rider")).toBeInTheDocument();
    expect(screen.getByText("Gravel Explorer 900")).toBeInTheDocument();
    expect(screen.getByText("vertrag versendet")).toBeInTheDocument();
  });

  it("asks the backend only for the status in the URL filter", async () => {
    let requestedStatus: string | null = null;
    server.use(
      http.get("*/api/bike-leasing", ({ request }) => {
        requestedStatus = new URL(request.url).searchParams.get("status");
        return HttpResponse.json(oneApplication);
      }),
    );
    renderWithProviders(page({ status: "CONTRACT_SENT" }));
    await screen.findByText("Alice Rider");
    expect(requestedStatus).toBe("CONTRACT_SENT");
  });

  it("shows the empty state with a call to action when there are no applications", async () => {
    server.use(
      http.get("*/api/bike-leasing", () =>
        HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      ),
    );
    const onNew = vi.fn();
    renderWithProviders(page({ onNew }));
    const cta = await screen.findByRole("button", { name: /ersten antrag/i });
    cta.click();
    expect(onNew).toHaveBeenCalled();
  });

  it("shows an error with a retry button when the request fails", async () => {
    server.use(
      http.get("*/api/bike-leasing", () => HttpResponse.json({ detail: "boom" }, { status: 500 })),
    );
    renderWithProviders(page());
    expect(await screen.findByRole("button", { name: /erneut versuchen/i })).toBeInTheDocument();
  });

  it("asks the backend for the zero-based index of the page in the URL", async () => {
    let requestedPage: string | null = null;
    server.use(
      http.get("*/api/bike-leasing", ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get("page");
        return HttpResponse.json(pageOf(1, 3));
      }),
    );
    renderWithProviders(page({ pageNumber: 2 }));
    await screen.findByText("Alice Rider");
    expect(requestedPage).toBe("1");
  });

  it("shows where it is and pages back and forth", async () => {
    const user = userEvent.setup();
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(pageOf(1, 3))));
    const onPageChange = vi.fn();
    renderWithProviders(page({ pageNumber: 2, onPageChange }));

    expect(await screen.findByText("seite 2 von 3")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "weiter" }));
    await user.click(screen.getByRole("button", { name: "zurück" }));
    expect(onPageChange.mock.calls).toEqual([[3], [1]]);
  });

  it("can't page before the first or past the last page", async () => {
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(pageOf(0, 2))));
    const { unmount } = renderWithProviders(page({ pageNumber: 1 }));
    await screen.findByText("seite 1 von 2");
    expect(screen.getByRole("button", { name: "zurück" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "weiter" })).toBeEnabled();
    unmount();

    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(pageOf(1, 2))));
    renderWithProviders(page({ pageNumber: 2 }));
    await screen.findByText("seite 2 von 2");
    expect(screen.getByRole("button", { name: "zurück" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "weiter" })).toBeDisabled();
  });

  it("hides the pager when everything fits on one page", async () => {
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(oneApplication)));
    renderWithProviders(page());
    await screen.findByText("Alice Rider");
    expect(screen.queryByRole("button", { name: "weiter" })).not.toBeInTheDocument();
  });

  it("polls so a case the engine advances shows its new status", async () => {
    vi.useFakeTimers();
    let count = 0;
    server.use(
      http.get("*/api/bike-leasing", () => {
        count += 1;
        return HttpResponse.json(oneApplication);
      }),
    );
    renderWithProviders(page());

    await vi.advanceTimersByTimeAsync(50);
    expect(count).toBe(1);
    await vi.advanceTimersByTimeAsync(4500);
    expect(count).toBeGreaterThan(1);
  });

  it.each(["{Enter}", " "])("opens a row from the keyboard with %s", async (key) => {
    const user = userEvent.setup();
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(oneApplication)));
    const onSelect = vi.fn();
    renderWithProviders(page({ onSelect }));
    await screen.findByText("Alice Rider");

    await user.tab();
    await user.tab();
    expect(screen.getByText("Alice Rider").closest("tr")).toHaveFocus();
    await user.keyboard(key);

    expect(onSelect).toHaveBeenCalledWith("app-1");
  });

  it("labels the status filter", async () => {
    server.use(http.get("*/api/bike-leasing", () => HttpResponse.json(oneApplication)));
    renderWithProviders(page());
    await screen.findByText("Alice Rider");
    expect(screen.getByLabelText("status")).toHaveRole("combobox");
  });
});
