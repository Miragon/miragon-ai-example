---
name: create-rest-controller
argument-hint: "[<path-to-UseCase-or-Query-port-file>] [http-verb] [path]"
description: Scaffold or update a Spring @RestController (inbound adapter) for one inbound port, with nested request/response types and a toCommand() mapping. Use when the user asks to "create a REST controller", "add an endpoint", "expose a use-case over REST", or "scaffold an inbound adapter". The controller obeys the hexagonal + naming architecture rules and carries an @Operation(operationId) so the generated frontend client stays stable.
allowed-tools: Read, Write, Glob
---

# Skill: create-rest-controller

Generate or update **one** `@RestController` for one inbound port in
`src/main/kotlin/io/miragon/blueprint/adapter/inbound/rest/`. One endpoint per class. See
[ADR-0002](../../../docs/adr/0002-hexagonal-architecture-for-the-backend.md) and
[ADR-0003](../../../docs/adr/0003-openapi-as-the-checked-in-contract.md).

## Pattern (this repo)

```kotlin
@RestController
@RequestMapping("/api/bike-leasing")
class CompleteCreditCheckController(
    private val useCase: CompleteCreditCheckUseCase,
) {
    @Operation(operationId = "completeCreditCheck", responses = [/* 202, 400, 404 as ProblemDetail */])
    @PostMapping("/{applicationId}/credit-check")
    fun completeCreditCheck(
        @PathVariable applicationId: String,
        @RequestBody input: CreditCheckInput,
    ): ResponseEntity<Void> {
        useCase.completeCreditCheck(ApplicationId.of(applicationId), input.solvent)
        return ResponseEntity.accepted().build()
    }

    data class CreditCheckInput
        @JsonCreator
        constructor(@JsonProperty("solvent", required = true) val solvent: Boolean)
}
```

A **read** controller injects a `*Query` and maps to a `*Dto` (see `ListBikesController`,
`LeasingApplicationDto`).

## IMPORTANT — the rules the build enforces

- **One endpoint per controller**, and the port is the **only** inbound-port constructor parameter
  (`in adapters should only offer one use-case or query`). A read and a completion are two controllers.
- Suffixes in `adapter.inbound.rest`: `Controller`, `Dto`, `Input`, `Mapper`, `Configuration`.
- **Wrap every request field in its domain value object** before calling the port. Scan
  `domain/{leasing,bike}/` first; ask before creating a missing one.
- Request → `*Input`, response → `*Dto`, nested in the controller unless shared.
- Return `ResponseEntity<T>`; a command that continues asynchronously answers **202**.
- Errors are RFC 9457 problem details from `GlobalExceptionConfiguration`: `IllegalArgumentException`
  → 400, `IllegalStateException` → 404, `MismatchingMessageCorrelationException` → 409. Document them
  in `@Operation(responses = …)`.
- **Always set `@Operation(operationId = "…")`** (camelCase): orval derives the hook name from it.
- No new top-level `config` package; cross-cutting `@Configuration` goes in `adapter.inbound.rest`.

## Instructions

1. **Resolve the port** from `$ARGUMENTS`, else `Glob src/main/kotlin/**/application/port/inbound/*`,
   list and ask.
2. **Verb and path:** commands are `POST /api/bike-leasing/{applicationId}/<action>`, reads are `GET`
   (`/api/bike-leasing`, `/api/tasks`, `/api/bikes`). Confirm with the user.
3. **Generate or update** per the pattern; for an existing file add only what is missing.
4. **Report and hand off:** add a `@WebMvcTest` with the port mocked
   (`./gradlew test --tests "*<Name>ControllerTest"`), then run
   [`sync-api-client`](../sync-api-client/SKILL.md): the spec is **drift-gated in CI**. Add or update
   the Bruno scenario in `bruno/`.
