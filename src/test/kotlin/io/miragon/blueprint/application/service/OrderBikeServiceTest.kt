package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OrderBikeServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = OrderBikeService(repository = repository, bikeDealer = bikeDealer)

    @Test
    fun `orderBike orders the application's bike and moves it to ORDERED`() {
        val application = testLeasingApplication(bikeId = BikeId("BIKE-800"), status = LeasingStatus.CONTRACT_SENT)
        every { repository.findById(application.id) } returns application
        every { bikeDealer.order(BikeId("BIKE-800")) } returns OrderId("ORDER-800")
        every { repository.save(any()) } answers { firstArg() }

        val orderId = underTest.orderBike(application.id)

        assertThat(orderId).isEqualTo(OrderId("ORDER-800"))
        verify {
            repository.save(match { it.status == LeasingStatus.ORDERED && it.orderId == OrderId("ORDER-800") })
        }
    }

    @Test
    fun `orderBike lets a dealer failure through and stores nothing`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { bikeDealer.order(any()) } throws IllegalStateException("dealer down")

        assertThatThrownBy { underTest.orderBike(application.id) }.hasMessage("dealer down")
        verify(exactly = 0) { repository.save(any()) }
    }
}
