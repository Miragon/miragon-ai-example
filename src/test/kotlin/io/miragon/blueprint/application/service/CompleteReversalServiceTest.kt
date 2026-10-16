package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CompleteReversalServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val process = mockk<LeasingProcess>()
    private val underTest = CompleteReversalService(repository = repository, process = process)

    @Test
    fun `completeReversal completes the task and cancels the application`() {
        val application = testLeasingApplication(status = LeasingStatus.WITHDRAWN)
        every { repository.findById(application.id) } returns application
        every { process.completeReversal(application.id) } just Runs
        every { repository.save(any()) } answers { firstArg() }

        underTest.completeReversal(application.id)

        verifyOrder {
            process.completeReversal(application.id)
            repository.save(match { it.status == LeasingStatus.CANCELLED })
        }
    }

    @Test
    fun `completeReversal refuses an unknown application`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns null

        assertThatThrownBy { underTest.completeReversal(application.id) }.isInstanceOf(ApplicationNotFoundException::class.java)
        verify(exactly = 0) { process.completeReversal(any()) }
    }
}
