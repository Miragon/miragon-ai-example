package io.miragon.blueprint.adapter.outbound.dealer

import io.miragon.blueprint.domain.bike.BikeId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BikeDealerAdapterTest {

    private val underTest = BikeDealerAdapter()

    @Test
    fun `the catalogue offers the demo bikes including the outage bike`() {
        assertThat(underTest.catalogue().map { it.bikeId.value })
            .containsExactly("BIKE-900", "BIKE-800", "BIKE-600", "BIKE-FAIL")
    }

    @Test
    fun `order mints a fresh, unique order id`() {
        val first = underTest.order(BikeId("BIKE-900"))
        val second = underTest.order(BikeId("BIKE-900"))
        assertThat(first.value).startsWith("ORDER-")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `ordering the incident-demo bike fails as a simulated dealer outage`() {
        assertThatThrownBy { underTest.order(BikeId("BIKE-FAIL")) }
            .isInstanceOf(BikeDealerAdapter.DealerUnavailableException::class.java)
            .hasMessage("Bike dealer API unavailable for BIKE-FAIL (simulated demo outage)")
    }
}
