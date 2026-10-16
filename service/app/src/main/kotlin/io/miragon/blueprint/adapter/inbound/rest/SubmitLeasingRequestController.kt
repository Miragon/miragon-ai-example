package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import mu.KotlinLogging
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class SubmitLeasingRequestController(
    private val useCase: SubmitLeasingRequestUseCase,
) {

    private val log = KotlinLogging.logger {}

    @Operation(
        operationId = "submitLeasingRequest",
        responses = [
            ApiResponse(responseCode = "200", description = "The id of the new leasing application"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request, for example no income",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    fun submit(@RequestBody input: LeasingRequestInput): ResponseEntity<LeasingApplicationCreatedDto> {
        log.debug { "Received leasing request: $input" }
        val id = useCase.submit(input.toCommand())
        return ResponseEntity.ok(LeasingApplicationCreatedDto(id.value.toString()))
    }

    data class LeasingRequestInput
        @JsonCreator
        constructor(
            @JsonProperty("customerName") val customerName: String,
            @JsonProperty("email") val email: String,
            @JsonProperty("age") val age: Int,
            @JsonProperty("monthlyNetIncome") val monthlyNetIncome: Double,
            @JsonProperty("bikeId") val bikeId: String,
        )

    data class LeasingApplicationCreatedDto(val applicationId: String)

    private fun LeasingRequestInput.toCommand() =
        SubmitLeasingRequestUseCase.Command(
            customerName = CustomerName(customerName),
            email = Email(email),
            age = age,
            monthlyNetIncome = monthlyNetIncome,
            bikeId = BikeId(bikeId),
        )
}
