package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ListBikesServiceTest {

    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = ListBikesService(bikeDealer = bikeDealer)

    @Test
    fun `all returns the dealer's catalogue`() {
        val catalogue = listOf(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))
        every { bikeDealer.catalogue() } returns catalogue

        assertThat(underTest.all()).isEqualTo(catalogue)
    }
}
