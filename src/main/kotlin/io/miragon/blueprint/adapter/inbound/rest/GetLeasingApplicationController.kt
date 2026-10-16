package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class GetLeasingApplicationController(
    private val query: GetLeasingApplicationQuery,
) {

    @Operation(
        operationId = "getLeasingApplication",
        responses = [
            ApiResponse(responseCode = "200", description = "The leasing application"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid application id",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            // No body: hide the schema springdoc would otherwise copy from the return type.
            ApiResponse(
                responseCode = "404",
                description = "Unknown application (no body)",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    @GetMapping("/{applicationId}")
    fun byId(@PathVariable applicationId: String): ResponseEntity<LeasingApplicationDto> {
        val application = query.byId(ApplicationId.of(applicationId)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(LeasingApplicationDto.of(application))
    }
}
