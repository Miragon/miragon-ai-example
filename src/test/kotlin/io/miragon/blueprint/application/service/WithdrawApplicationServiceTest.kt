package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
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

class WithdrawApplicationServiceTest {

    private val process = mockk<LeasingProcess>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = WithdrawApplicationService(process = process, repository = repository)

    @Test
    fun `withdraw correlates the withdrawal and moves the application to WITHDRAWN`() {
        val application = testLeasingApplication(status = LeasingStatus.CONTRACT_SENT)
        every { process.correlateApplicationWithdrawn(application.id) } just Runs
        every { repository.findById(application.id) } returns application
        every { repository.save(any()) } answers { firstArg() }

        underTest.withdraw(application.id)

        verifyOrder {
            process.correlateApplicationWithdrawn(application.id)
            repository.save(match { it.status == LeasingStatus.WITHDRAWN })
        }
    }

    @Test
    fun `withdraw keeps the status when the process refuses the withdrawal`() {
        val application = testLeasingApplication(status = LeasingStatus.ACTIVE)
        every { process.correlateApplicationWithdrawn(application.id) } throws IllegalStateException("finished")

        assertThatThrownBy { underTest.withdraw(application.id) }.hasMessage("finished")
        verify(exactly = 0) { repository.save(any()) }
    }
}
