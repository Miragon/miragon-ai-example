package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.miragon.blueprint.application.port.inbound.CompleteCreditCheckUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class CompleteCreditCheckController(
    private val useCase: CompleteCreditCheckUseCase,
) {

    @Operation(
        operationId = "completeCreditCheck",
        responses = [
            ApiResponse(
                responseCode = "202",
                description = "Accepted: the task is completed, the process continues asynchronously",
            ),
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
                description = "No open credit check for this application",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
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
        constructor(
            @JsonProperty("solvent", required = true) val solvent: Boolean,
        )
}
