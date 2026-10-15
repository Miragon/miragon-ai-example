package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SelectAlternativeServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val process = mockk<LeasingProcess>()
    private val underTest = SelectAlternativeService(repository = repository, process = process)

    @Test
    fun `an accepted alternative hands the new bike to the process`() {

        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { process.completeAlternativeClarification(any(), any(), any()) } just Runs

        // when: an alternative bike is selected
        underTest.selectAlternative(
            SelectAlternativeUseCase.Command(application.id, alternativeFound = true, bikeId = BikeId("BIKE-ALT")),
        )

        // then: the task is completed with the alternative; the application itself is left to the re-order
        verify { repository.findById(application.id) }
        verify { process.completeAlternativeClarification(application.id, true, BikeId("BIKE-ALT")) }
        confirmVerified(repository, process)
    }

    @Test
    fun `no alternative completes the user task without a bike`() {

        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { process.completeAlternativeClarification(any(), any(), any()) } just Runs

        // when: no alternative is found
        underTest.selectAlternative(SelectAlternativeUseCase.Command(application.id, alternativeFound = false))

        // then: the task is completed as declined, without a bike
        verify { repository.findById(application.id) }
        verify { process.completeAlternativeClarification(application.id, false, null) }
        confirmVerified(repository, process)
    }

    @Test
    fun `an unknown application is refused before the process is touched`() {

        // given: an id the repository cannot resolve
        val id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000")
        every { repository.findById(id) } returns null

        // when / then: the selection fails and no task is completed
        assertThatThrownBy {
            underTest.selectAlternative(SelectAlternativeUseCase.Command(id, alternativeFound = true, bikeId = BikeId("BIKE-ALT")))
        }.isInstanceOf(IllegalStateException::class.java)
        verify { repository.findById(id) }
        confirmVerified(repository, process)
    }
}
