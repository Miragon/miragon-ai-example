package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.SignContractUseCase
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
class SignContractController(
    private val useCase: SignContractUseCase,
) {

    @Operation(
        operationId = "signContract",
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
                responseCode = "409",
                description = "The case is not waiting for a signature",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{applicationId}/sign-contract")
    fun signContract(@PathVariable applicationId: String): ResponseEntity<Void> {
        useCase.signContract(ApplicationId.of(applicationId))
        return ResponseEntity.accepted().build()
    }
}
