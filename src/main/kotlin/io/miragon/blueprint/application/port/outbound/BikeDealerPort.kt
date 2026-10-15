package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId

/**
 * Outbound port to the (simulated) bike dealer: it answers whether a bike is in stock and places the
 * order. Implemented by an infrastructure adapter, so the dealer rules live outside the domain.
 */
interface BikeDealerPort {
    fun checkAvailability(bikeId: BikeId): Boolean

    fun order(bikeId: BikeId): OrderId
}
