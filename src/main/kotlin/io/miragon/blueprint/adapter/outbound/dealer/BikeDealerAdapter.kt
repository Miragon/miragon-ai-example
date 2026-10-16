package io.miragon.blueprint.adapter.outbound.dealer

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Simulated bike dealer with a fixed catalogue. `BIKE-FAIL` is the demo bike whose order fails as if
 * the dealer's API were unreachable: `serviceTask_orderBike` runs as its own job (asyncBefore, retry
 * cycle R3/PT10S), so the failure exhausts its retries and raises an incident (`06-incident-demo`).
 */
@Component
class BikeDealerAdapter : BikeDealerPort {

    private val log = KotlinLogging.logger {}

    private val catalogue =
        listOf(
            Bike(BikeId("BIKE-900"), "Gravel Explorer 900"),
            Bike(BikeId("BIKE-800"), "Carbon Road 800"),
            Bike(BikeId("BIKE-600"), "Mountain Trail 600"),
            Bike(BikeId("BIKE-FAIL"), "Prototype X (dealer outage demo)"),
        )

    private val unreachableDealerBikeIds = setOf(BikeId("BIKE-FAIL"))

    override fun catalogue(): List<Bike> = catalogue

    override fun order(bikeId: BikeId): OrderId {
        if (bikeId in unreachableDealerBikeIds) throw DealerUnavailableException(bikeId)
        val orderId = OrderId("ORDER-${UUID.randomUUID()}")
        log.info { "Placed order ${orderId.value} for bike ${bikeId.value}" }
        return orderId
    }

    class DealerUnavailableException(bikeId: BikeId) :
        RuntimeException("Bike dealer API unavailable for ${bikeId.value} (simulated demo outage)")
}
