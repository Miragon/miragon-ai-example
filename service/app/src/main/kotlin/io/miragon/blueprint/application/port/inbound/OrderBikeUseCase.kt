package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId

/** Orders the application's bike from the dealer. */
fun interface OrderBikeUseCase {
    fun orderBike(id: ApplicationId): OrderId
}
