package io.miragon.blueprint.domain.bike

/** A bike the dealer offers for leasing. */
data class Bike(
    val bikeId: BikeId,
    val model: String,
)
