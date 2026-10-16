package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.Bike

/** The dealer's bike catalogue, offered in the leasing request form. */
fun interface ListBikesQuery {
    fun all(): List<Bike>
}
