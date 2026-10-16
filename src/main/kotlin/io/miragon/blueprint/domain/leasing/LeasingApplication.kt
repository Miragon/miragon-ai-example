package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import java.time.LocalDateTime

/**
 * Aggregate root of the bike-leasing domain. All state transitions return a copy, so an instance is
 * never mutated in place — the calling service persists the returned copy.
 */
data class LeasingApplication(
    val id: ApplicationId,
    val customerName: CustomerName,
    val email: Email,
    val age: Int,
    val monthlyNetIncome: Double,
    val bikeId: BikeId,
    val bikeModel: String?,
    val status: LeasingStatus,
    val createdAt: LocalDateTime,
    val orderId: OrderId? = null,
    val contractId: ContractId? = null,
    val policyId: PolicyId? = null,
) {

    fun validate() {
        val violations = buildList {
            if (age < ADULT_AGE) add("the applicant must be at least $ADULT_AGE years old")
            if (bikeModel == null) add("bike ${bikeId.value} is not in the catalogue")
        }
        if (violations.isNotEmpty()) throw ApplicationInvalidException(id, violations)
    }

    fun sendContract(contractId: ContractId): LeasingApplication =
        copy(contractId = contractId, status = LeasingStatus.CONTRACT_SENT)

    fun documentOrder(orderId: OrderId): LeasingApplication =
        copy(orderId = orderId, status = LeasingStatus.ORDERED)

    fun documentPolicy(policyId: PolicyId): LeasingApplication = copy(policyId = policyId)

    fun activate(): LeasingApplication = copy(status = LeasingStatus.ACTIVE)

    fun reject(): LeasingApplication = copy(status = LeasingStatus.REJECTED)

    fun withdraw(): LeasingApplication = copy(status = LeasingStatus.WITHDRAWN)

    fun cancel(): LeasingApplication = copy(status = LeasingStatus.CANCELLED)

    companion object {
        const val ADULT_AGE = 18

        fun receive(
            id: ApplicationId,
            customerName: CustomerName,
            email: Email,
            age: Int,
            monthlyNetIncome: Double,
            bikeId: BikeId,
            bikeModel: String?,
            createdAt: LocalDateTime,
        ): LeasingApplication {
            require(monthlyNetIncome > 0.0) { "Monthly net income must be greater than zero" }
            return LeasingApplication(
                id = id,
                customerName = customerName,
                email = email,
                age = age,
                monthlyNetIncome = monthlyNetIncome,
                bikeId = bikeId,
                bikeModel = bikeModel,
                status = LeasingStatus.RECEIVED,
                createdAt = createdAt,
            )
        }
    }
}
