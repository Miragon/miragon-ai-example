---
name: create-rest-controller
argument-hint: "[<path-to-UseCase-or-Query-port-file>] [http-verb] [path]"
description: "Create or update a Spring REST controller for one inbound port with its @WebMvcTest, carried through openapi.json to the frontend client. Use when adding an endpoint or exposing a use case over REST."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./gradlew *), Bash(git diff *), Bash(npm --prefix frontend run *)
---

# Skill: create-rest-controller

One endpoint per class in `service/app/src/main/kotlin/io/miragon/blueprint/adapter/inbound/rest/`,
its test in `service/app/src/test/kotlin/io/miragon/blueprint/adapter/inbound/rest/`. The backend
rules are in `service/AGENTS.md`.

## Production pattern

```kotlin
@RestController
@RequestMapping("/api/bike-leasing")
class CompleteCreditCheckController(
    private val useCase: CompleteCreditCheckUseCase,
) {
    @Operation(operationId = "completeCreditCheck", responses = [/* 202, 400, 404, 409 as ProblemDetail */])
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

- **One inbound port** as the only constructor parameter; a read and a command are two controllers.
- Commands are `POST /api/bike-leasing/{applicationId}/<action>` and answer **202** when the engine
  continues asynchronously. Reads are `GET` with a `*Query` and a `*Dto` (see `ListBikesController`,
  `LeasingApplicationDto`).
- Wrap every request field in its domain value object before calling the port.
- **Always set `@Operation(operationId = "…")`** (camelCase): orval derives the hook name from it.
- Errors are problem details from `GlobalExceptionConfiguration`: `IllegalArgumentException` → 400,
  `ApplicationNotFoundException` → 404, `ActionNotAvailableException`,
  `MismatchingMessageCorrelationException`, `OptimisticLockingException` → 409. Document them in
  `@Operation(responses = …)`.

## Test pattern

```kotlin
@WebMvcTest(CompleteCreditCheckController::class)
class CompleteCreditCheckControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: CompleteCreditCheckUseCase

    private val pathVar = "123e4567-e89b-12d3-a456-426614174000"

    private fun creditCheck(body: String) =
        post("/api/bike-leasing/{applicationId}/credit-check", pathVar)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

    @Test
    fun `back office records a solvent applicant`() {
        every { useCase.completeCreditCheck(any(), any()) } just Runs

        val response = mockMvc.perform(creditCheck("""{"solvent":true}""")).andReturn()

        assertThat(response.response.status).isEqualTo(202)
        verify { useCase.completeCreditCheck(ApplicationId.of(pathVar), true) }
        confirmVerified(useCase)
    }

    @Test
    fun `refuses a decision without the solvent flag`() {
        val response = mockMvc.perform(creditCheck("{}")).andReturn()

        assertThat(response.response.status).isEqualTo(400)
        confirmVerified(useCase)
    }
}
```

- One test per outcome: the success status with the exact port call, each invalid input (400), and
  the not-found case where the port can throw it (404, see `GetLeasingApplicationControllerTest`).
  The exception-to-status mapping itself is tested once in `GlobalExceptionConfigurationTest`.
- Verify the port call with the domain value objects, then `confirmVerified`; a rejected request
  proves the port was **not** called.
- A read controller asserts every DTO field in the body, including the date format (see
  `GetLeasingApplicationControllerTest`).

## Instructions

1. **Resolve the port** from `$ARGUMENTS`, else list `application/port/inbound/*` and ask.
2. **Agree verb, path and status codes** with the user.
3. **Write the test first**, then the controller; for an existing file add only what is missing.
   `./gradlew test --tests "*<Name>ControllerTest"`
4. **Carry the contract through** (both sides are drift-gated in CI; never hand-edit the outputs):
   ```bash
   ./gradlew test --tests "io.miragon.blueprint.adapter.inbound.rest.OpenApiSpecExportTest"
   git diff --stat -- openapi/openapi.json
   npm --prefix frontend run api:generate
   npm --prefix frontend run typecheck
   npm --prefix frontend run api:check
   ```
   An endpoint missing from the spec lacks its `@Operation(operationId)`.
5. **Report** the files, the test results and the spec diff. Add the request to the matching Bruno
   folder in `bruno/`; the UI follows with skill `create-feature-slice`.
