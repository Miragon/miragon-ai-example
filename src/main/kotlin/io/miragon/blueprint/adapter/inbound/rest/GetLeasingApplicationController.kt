package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
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
import java.time.LocalDateTime

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
        return ResponseEntity.ok(application.toDto())
    }

    data class LeasingApplicationDto(
        val applicationId: String,
        val customerName: String,
        val email: String,
        val age: Int,
        val monthlyNetIncome: Double,
        val bikeId: String,
        val status: String,
        val orderId: String?,
        val contractId: String?,
        // Force ISO-8601 string form: Jackson 3 (SB4) defaults to a numeric array, but the CIB seven
        // webapp serves /api with its own Jackson mapper that ignores our global date-time config, so
        // the format is pinned at the field to keep the payload in sync with the springdoc contract.
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val createdAt: LocalDateTime,
    )

    private fun LeasingApplication.toDto() =
        LeasingApplicationDto(
            applicationId = id.value.toString(),
            customerName = customerName.value,
            email = email.value,
            age = age,
            monthlyNetIncome = monthlyNetIncome,
            bikeId = bikeId.value,
            status = status.name,
            orderId = orderId?.value,
            contractId = contractId?.value,
            createdAt = createdAt,
        )
}
