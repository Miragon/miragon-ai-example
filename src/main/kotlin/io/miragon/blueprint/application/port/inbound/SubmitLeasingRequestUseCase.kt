package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email

/** Receives a customer's leasing request and starts the bike-leasing process for it. */
interface SubmitLeasingRequestUseCase {
    fun submit(command: Command): ApplicationId

    /** The request as the customer submits it; the bike is identified by its [bikeId] only. */
    data class Command(
        val customerName: CustomerName,
        val email: Email,
        val age: Int,
        val monthlyNetIncome: Double,
        val bikeId: BikeId,
    )
}
