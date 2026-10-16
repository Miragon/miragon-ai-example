package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class ReportHandoverController(
    private val useCase: ReportHandoverUseCase,
) {

    @Operation(
        operationId = "reportHandover",
        responses = [
            ApiResponse(responseCode = "202", description = "Accepted: the process continues asynchronously"),
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
            ApiResponse(
                responseCode = "404",
                description = "Unknown application",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "The case is not waiting for the handover",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{applicationId}/report-handover")
    fun reportHandover(@PathVariable applicationId: String): ResponseEntity<Void> {
        useCase.reportHandover(ApplicationId.of(applicationId))
        return ResponseEntity.accepted().build()
    }
}
