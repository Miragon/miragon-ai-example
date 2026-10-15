package io.miragon.blueprint.adapter.outbound.dealer

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Simulated bike dealer. Two demo bike ids make its behaviour deterministic, and the Bruno scenarios
 * use them: `BIKE-OOS` is out of stock, which drives the bike-unavailable → alternative branch
 * (`04-bike-unavailable`, `05-alternative-declined`, `06-alternative-via-tasklist`), and `BIKE-FAIL`
 * is the poison id that makes the dealer "outage" throw (`08-incident-demo`) — see [order]. Every
 * other bike id is in stock.
 */
@Component
class BikeDealerAdapter : BikeDealerPort {

    private val log = KotlinLogging.logger {}

    private val outOfStockBikeIds = setOf("BIKE-OOS")

    /**
     * Demo mechanism (not a real integration fault): ordering one of these bikes makes the dealer
     * call fail as if the dealer's API were unreachable. `serviceTask_orderBike` runs as its own job
     * on the job executor (asyncBefore, retry cycle R3/PT10S), so the failure exhausts its retries and
     * raises an incident — the basis of the reproducible incident demo. It stays available in
     * [checkAvailability] so the flow reaches the order step (rather than the out-of-stock branch)
     * before failing.
     */
    private val unavailableDealerBikeIds = setOf("BIKE-FAIL")

    override fun checkAvailability(bikeId: BikeId): Boolean = bikeId.value !in outOfStockBikeIds

    override fun order(bikeId: BikeId): OrderId {
        if (bikeId.value in unavailableDealerBikeIds) throw DealerUnavailableException(bikeId)
        val orderId = OrderId("ORDER-${UUID.randomUUID()}")
        log.info { "Placed order ${orderId.value} for bike ${bikeId.value}" }
        return orderId
    }

    /**
     * Signals that the (simulated) bike dealer could not be reached while placing an order. Thrown for
     * the `BIKE-FAIL` poison id to drive the reproducible incident demo: because `serviceTask_orderBike`
     * runs on the job executor, this surfaces as a failed job that retries and then raises an incident.
     * Nested so it stays colocated with the demo logic without needing an outbound-adapter naming suffix.
     */
    class DealerUnavailableException(bikeId: BikeId) :
        RuntimeException("Bike dealer API unavailable for ${bikeId.value} (simulated demo outage)")
}
