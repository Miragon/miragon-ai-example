package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ActionNotAvailableException
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

class SignContractServiceTest {

    private val process = mockk<LeasingProcess>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = SignContractService(process = process, repository = repository)

    @Test
    fun `signContract correlates the signature and moves the application to CONTRACT_SIGNED`() {
        val application = testLeasingApplication(status = LeasingStatus.CONTRACT_SENT)
        every { repository.findById(application.id) } returns application
        every { process.correlateContractSigned(application.id) } just Runs
        every { repository.save(any()) } answers { firstArg() }

        underTest.signContract(application.id)

        verifyOrder {
            process.correlateContractSigned(application.id)
            repository.save(match { it.status == LeasingStatus.CONTRACT_SIGNED })
        }
    }

    @Test
    fun `signContract refuses a contract that is not waiting for a signature`() {
        val application = testLeasingApplication(status = LeasingStatus.CONTRACT_SIGNED)
        every { repository.findById(application.id) } returns application

        assertThatThrownBy { underTest.signContract(application.id) }
            .isInstanceOf(ActionNotAvailableException::class.java)
        verify(exactly = 0) { process.correlateContractSigned(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `signContract refuses an unknown application`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns null

        assertThatThrownBy { underTest.signContract(application.id) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
        verify(exactly = 0) { process.correlateContractSigned(any()) }
    }
}
