import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { z } from "zod";
import { LEASING_STATUSES } from "@/entities/leasing-application";
import { ApplicationListPage } from "@/pages/application-list";

// The status filter and the page live in the URL, so they survive reload and are shareable.
const searchSchema = z.object({
  status: z.enum(LEASING_STATUSES).optional(),
  page: z.number().int().min(1).optional(),
});

export const Route = createFileRoute("/")({
  component: ApplicationListRoute,
  validateSearch: searchSchema,
});

function ApplicationListRoute() {
  const navigate = useNavigate();
  const { status, page } = Route.useSearch();

  return (
    <ApplicationListPage
      status={status ?? null}
      onStatusChange={(next) => void navigate({ to: "/", search: next ? { status: next } : {} })}
      pageNumber={page ?? 1}
      onPageChange={(next) => void navigate({ to: "/", search: { status, page: next } })}
      onSelect={(applicationId) =>
        void navigate({ to: "/antraege/$applicationId", params: { applicationId } })
      }
      onNew={() => void navigate({ to: "/antraege/neu" })}
    />
  );
}
