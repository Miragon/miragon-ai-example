import { afterEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { renderWithProviders, screen, server } from "@/shared/testing";
import { ApplicationDetailPage } from "./application-detail-page";

function application(status: string) {
  return {
    applicationId: "app-1",
    customerName: "Alice Rider",
    email: "alice@example.com",
    age: 35,
    monthlyNetIncome: 3500,
    bikeId: "BIKE-900",
    bikeModel: "Gravel Explorer 900",
    status,
    contractId: null,
    orderId: null,
    policyId: null,
    createdAt: "2026-08-17T10:30:00Z",
  };
}

describe("ApplicationDetailPage polling", () => {
  afterEach(() => vi.useRealTimers());

  it("does NOT poll once the case is terminal (ACTIVE)", async () => {
    vi.useFakeTimers();
    let count = 0;
    server.use(
      http.get("*/api/bike-leasing/:id", () => {
        count += 1;
        return HttpResponse.json(application("ACTIVE"));
      }),
    );

    renderWithProviders(<ApplicationDetailPage applicationId="app-1" />);

    await vi.advanceTimersByTimeAsync(50);
    expect(screen.getByText("Alice Rider")).toBeInTheDocument();

    await vi.advanceTimersByTimeAsync(6000);
    expect(count).toBe(1);
  });

  it("keeps polling while the back office reverses a withdrawal (WITHDRAWN)", async () => {
    vi.useFakeTimers();
    let count = 0;
    server.use(
      http.get("*/api/bike-leasing/:id", () => {
        count += 1;
        return HttpResponse.json(application("WITHDRAWN"));
      }),
    );

    renderWithProviders(<ApplicationDetailPage applicationId="app-1" />);

    await vi.advanceTimersByTimeAsync(50);
    await vi.advanceTimersByTimeAsync(4500);
    expect(count).toBeGreaterThan(1);
  });

  it("keeps polling while the case is non-terminal (RECEIVED)", async () => {
    vi.useFakeTimers();
    let count = 0;
    server.use(
      http.get("*/api/bike-leasing/:id", () => {
        count += 1;
        return HttpResponse.json(application("RECEIVED"));
      }),
    );

    renderWithProviders(<ApplicationDetailPage applicationId="app-1" />);

    await vi.advanceTimersByTimeAsync(50);
    expect(count).toBe(1);
    await vi.advanceTimersByTimeAsync(4500);
    expect(count).toBeGreaterThan(1);
  });
});
