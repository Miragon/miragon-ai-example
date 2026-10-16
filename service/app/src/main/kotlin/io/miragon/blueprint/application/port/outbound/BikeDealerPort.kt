package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId

/**
 * Outbound port to the (simulated) bike dealer: it offers the bike catalogue and places the order.
 * Implemented by an infrastructure adapter, so the dealer rules live outside the domain.
 */
interface BikeDealerPort {
    fun catalogue(): List<Bike>

    fun order(bikeId: BikeId): OrderId
}
