package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.Bike
import org.springframework.stereotype.Service

@Service
class ListBikesService(
    private val bikeDealer: BikeDealerPort,
) : ListBikesQuery {

    override fun all(): List<Bike> = bikeDealer.catalogue()
}
