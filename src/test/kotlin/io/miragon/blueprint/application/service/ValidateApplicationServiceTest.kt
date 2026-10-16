package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ValidateApplicationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ValidateApplicationService(repository = repository)

    @Test
    fun `validate passes a valid application`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application

        assertThatCode { underTest.validate(application.id) }.doesNotThrowAnyException()
    }

    @Test
    fun `validate reports an invalid application`() {
        val application = testLeasingApplication(age = 17)
        every { repository.findById(application.id) } returns application

        assertThatThrownBy { underTest.validate(application.id) }.isInstanceOf(ApplicationInvalidException::class.java)
    }

    @Test
    fun `validate fails for an unknown application`() {
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns null

        assertThatThrownBy { underTest.validate(application.id) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unknown application")
    }
}
