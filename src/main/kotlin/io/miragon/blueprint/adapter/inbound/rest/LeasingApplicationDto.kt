package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import java.time.LocalDateTime

data class LeasingApplicationDto(
    val applicationId: String,
    val customerName: String,
    val email: String,
    val age: Int,
    val monthlyNetIncome: Double,
    val bikeId: String,
    val bikeModel: String?,
    val status: LeasingStatus,
    val contractId: String?,
    val orderId: String?,
    val policyId: String?,
    // Force ISO-8601 string form: Jackson 3 (SB4) defaults to a numeric array, but the CIB seven
    // webapp serves /api with its own Jackson mapper that ignores our global date-time config, so
    // the format is pinned at the field to keep the payload in sync with the springdoc contract.
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(application: LeasingApplication) =
            LeasingApplicationDto(
                applicationId = application.id.value.toString(),
                customerName = application.customerName.value,
                email = application.email.value,
                age = application.age,
                monthlyNetIncome = application.monthlyNetIncome,
                bikeId = application.bikeId.value,
                bikeModel = application.bikeModel,
                status = application.status,
                contractId = application.contractId?.value,
                orderId = application.orderId?.value,
                policyId = application.policyId?.value,
                createdAt = application.createdAt,
            )
    }
}
