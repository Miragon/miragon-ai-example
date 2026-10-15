package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
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

/**
 * External completion of the `Clarify alternative with customer` user task — the counterpart to a
 * human completing the deployed Camunda Form in the CIB seven Tasklist.
 */
@RestController
@RequestMapping("/api/bike-leasing")
class SelectAlternativeController(
    private val useCase: SelectAlternativeUseCase,
) {

    @Operation(
        operationId = "selectAlternative",
        responses = [
            ApiResponse(
                responseCode = "202",
                description = "Accepted: the task is completed, the process continues asynchronously",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid application id, or an accepted alternative without a bike id",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Unknown application, or no open clarification task",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{applicationId}/clarify-alternative")
    fun clarifyAlternative(
        @PathVariable applicationId: String,
        @RequestBody input: AlternativeDecisionInput,
    ): ResponseEntity<Void> {
        useCase.selectAlternative(
            SelectAlternativeUseCase.Command(
                applicationId = ApplicationId.of(applicationId),
                alternativeFound = input.alternativeFound,
                bikeId = input.bikeId?.let { BikeId(it) },
            ),
        )
        return ResponseEntity.accepted().build()
    }

    data class AlternativeDecisionInput
        @JsonCreator
        constructor(
            @JsonProperty("alternativeFound") val alternativeFound: Boolean,
            @JsonProperty("bikeId") val bikeId: String? = null,
        )
}
