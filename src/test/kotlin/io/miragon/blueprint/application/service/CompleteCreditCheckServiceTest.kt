package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CompleteCreditCheckServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val process = mockk<LeasingProcess>()
    private val underTest = CompleteCreditCheckService(repository = repository, process = process)

    @Test
    fun `completeCreditCheck completes the task with the decision`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { process.completeCreditCheck(application.id, false) } just Runs

        underTest.completeCreditCheck(application.id, solvent = false)

        verify { process.completeCreditCheck(application.id, false) }
        confirmVerified(process)
    }

    @Test
    fun `completeCreditCheck refuses an unknown application`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns null

        assertThatThrownBy { underTest.completeCreditCheck(application.id, solvent = true) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
        verify(exactly = 0) { process.completeCreditCheck(any(), any()) }
    }
}
