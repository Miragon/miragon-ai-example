---
name: create-feature-slice
argument-hint: "[<layer>/<slice-name>]  e.g. features/sign-contract or entities/bike"
description: "Create a Feature-Sliced Design slice under frontend/src with its Vitest test against the generated MSW handlers. Use when adding a frontend feature, entity, widget, page or an action for an endpoint."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(npm --prefix frontend run *)
---

# Skill: create-feature-slice

One slice under `frontend/src/<layer>/<slice-name>/`, its tests next to the code. The rules are in
`frontend/AGENTS.md` (layers, generated-client restriction, copy, polling, 409 handling). If the
backend endpoint is new, run skill `create-rest-controller` first so the generated client has it.

## Production pattern (a feature: one write)

```ts
// features/sign-contract/api/use-sign-contract.ts
export function useSignContractAction(applicationId: string) {
  const copy = useCopy();
  const queryClient = useQueryClient();
  const refreshApplication = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.all }),
      queryClient.invalidateQueries({ queryKey: leasingApplicationKeys.detail(applicationId) }),
      queryClient.invalidateQueries({ queryKey: openTaskKeys.all }),
    ]);
  return useSignContract({
    mutation: {
      onSuccess: async () => {
        await refreshApplication();
        toast.success(copy.actions.signSuccess);
      },
      onError: (error) => {
        if (!isConflict(error)) {
          toast.error(copy.actions.error);
          return;
        }
        void refreshApplication();
        toast.error(copy.common.conflictToast);
      },
    },
  });
}
```

```tsx
// features/sign-contract/ui/sign-contract-button.tsx
export function SignContractButton({ applicationId }: { applicationId: string }) {
  const copy = useCopy();
  const mutation = useSignContractAction(applicationId);
  const busy = mutation.isPending || mutation.isSuccess;
  return (
    <Button onClick={() => mutation.mutate({ applicationId })} disabled={busy}>
      {busy ? copy.actions.signing : copy.actions.sign}
    </Button>
  );
}
```

```ts
// features/sign-contract/index.ts
export { SignContractButton } from "./ui/sign-contract-button";
```

- Layers import only downward: `app → pages → widgets → features → entities → shared`; no
  `processes/`. An **entity** owns reads (re-export the generated query hook from `api/`), a
  **feature** owns one write with its invalidation and toasts.
- Only `entities/*/api` and `features/*/api` import `@/shared/api/generated`.
- Copy comes from `useCopy()`; add keys to `shared/i18n/de.ts` and `en.ts`.

## Test pattern

```tsx
// features/sign-contract/ui/sign-contract-button.test.tsx
function renderButton() {
  return renderWithProviders(
    <>
      <SignContractButton applicationId="app-1" />
      <Toaster />
    </>,
  );
}

describe("SignContractButton", () => {
  it("on a 409 says the application changed and re-reads it instead of showing the raw detail", async () => {
    const user = userEvent.setup({ pointerEventsCheck: 0 });
    server.use(
      http.post("*/api/bike-leasing/:id/sign-contract", () =>
        HttpResponse.json({ status: 409, detail: "raw engine detail" }, { status: 409 }),
      ),
    );
    const { queryClient } = renderButton();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    await user.click(screen.getByRole("button", { name: "vertrag unterschreiben" }));

    expect(await screen.findByText("der antrag hat sich inzwischen geändert.")).toBeInTheDocument();
    expect(screen.queryByText("raw engine detail")).not.toBeInTheDocument();
    expect(invalidate.mock.calls.map(([filters]) => filters?.queryKey)).toEqual(
      expect.arrayContaining([leasingApplicationKeys.all, openTaskKeys.all]),
    );
  });
});
```

- `renderWithProviders`, `server` and `screen` come from `@/shared/testing`; the generated MSW
  handlers answer by default, `server.use(…)` overrides one response per test.
- Act and assert like a user: `getByRole` with the visible German name, `findBy…` for what appears
  asynchronously.
- One test per outcome the user can see: success toast and invalidation, the 409 conflict path, the
  generic error, the busy state. Pure logic in `model/` gets a plain unit test.

## Instructions

1. **Resolve layer and name** (kebab-case) from `$ARGUMENTS`, else ask. Reject `processes/`.
2. **Write the test first**, then the segments the slice needs and its `index.ts` with only the
   public surface.
3. **Wire it in** from the layer above; a slice nothing imports fails knip.
4. **Verify:** `npm --prefix frontend run verify` (format, lint, steiger, knip, typecheck, tests). A
   flow users rely on also gets a Playwright spec in `frontend/e2e/` (see `frontend/AGENTS.md`).
