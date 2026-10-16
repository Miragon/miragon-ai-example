package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SubmitLeasingRequestServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val process = mockk<LeasingProcess>()
    private val bikeDealer = mockk<BikeDealerPort>()
    private val clock = Clock.fixed(Instant.parse("2024-01-15T10:30:00Z"), ZoneOffset.UTC)
    private val underTest =
        SubmitLeasingRequestService(
            repository = repository,
            process = process,
            bikeDealer = bikeDealer,
            clock = clock,
        )

    private fun command(bikeId: String = "BIKE-900", income: Double = 3500.0) =
        SubmitLeasingRequestUseCase.Command(
            customerName = CustomerName("John Doe"),
            email = Email("john.doe@test.com"),
            age = 35,
            monthlyNetIncome = income,
            bikeId = BikeId(bikeId),
        )

    @Test
    fun `submit persists a received application with the bike model and starts the process`() {

        // given: a request for a catalogue bike
        every { bikeDealer.catalogue() } returns
            listOf(Bike(BikeId("BIKE-800"), "Carbon Road 800"), Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))
        every { repository.save(any()) } answers { firstArg() }
        every { process.submitRequest(any()) } just Runs

        // when: the use case is invoked
        val id = underTest.submit(command())

        // then: a RECEIVED application with the model from the catalogue is saved, and the process starts
        verify {
            repository.save(
                match {
                    it.id == id &&
                        it.status == LeasingStatus.RECEIVED &&
                        it.customerName == CustomerName("John Doe") &&
                        it.email == Email("john.doe@test.com") &&
                        it.age == 35 &&
                        it.monthlyNetIncome == 3500.0 &&
                        it.bikeId == BikeId("BIKE-900") &&
                        it.bikeModel == "Gravel Explorer 900" &&
                        it.createdAt == LocalDateTime.now(clock)
                },
            )
        }
        verify { process.submitRequest(match { it.id == id && it.bikeModel == "Gravel Explorer 900" }) }
        confirmVerified(repository, process)
    }

    @Test
    fun `submit accepts a bike outside the catalogue and leaves it to the validation`() {

        // given: a request for a bike the dealer does not offer
        every { bikeDealer.catalogue() } returns listOf(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))
        every { repository.save(any()) } answers { firstArg() }
        every { process.submitRequest(any()) } just Runs

        // when: the use case is invoked
        underTest.submit(command(bikeId = "BIKE-X"))

        // then: the application is stored without a model and the process starts anyway
        verify { repository.save(match { it.bikeId == BikeId("BIKE-X") && it.bikeModel == null }) }
        verify { process.submitRequest(any()) }
    }

    @Test
    fun `submit rejects a request without income and starts no process`() {

        // given: a leasing-request command without any income
        every { bikeDealer.catalogue() } returns emptyList()

        // when / then: the request is refused before anything is persisted or started
        assertThatThrownBy { underTest.submit(command(income = 0.0)) }.isInstanceOf(IllegalArgumentException::class.java)
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { process.submitRequest(any()) }
    }
}
