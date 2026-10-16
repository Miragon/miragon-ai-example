package io.miragon.blueprint.adapter.inbound.cibseven

import io.miragon.blueprint.adapter.process.Errors
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.cibseven.bpm.engine.delegate.BpmnError
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidateApplicationDelegateTest {

    private val useCase = mockk<ValidateApplicationUseCase>()
    private val execution = mockk<DelegateExecution>()
    private val underTest = ValidateApplicationDelegate(useCase)

    private val applicationId = ApplicationId.new()

    @BeforeEach
    fun givenTheApplicationIsTheBusinessKey() {
        every { execution.processBusinessKey } returns applicationId.value.toString()
    }

    @Test
    fun `validates the application of the business key`() {
        every { useCase.validate(applicationId) } just Runs

        underTest.execute(execution)

        verify { useCase.validate(applicationId) }
        confirmVerified(useCase)
    }

    @Test
    fun `turns an invalid application into the applicationInvalid BPMN error`() {
        every { useCase.validate(applicationId) } throws
            ApplicationInvalidException(applicationId, listOf("customer is underage"))

        assertThatThrownBy { underTest.execute(execution) }
            .isInstanceOf(BpmnError::class.java)
            .extracting("errorCode")
            .isEqualTo(Errors.APPLICATION_INVALID.code)
    }

    @Test
    fun `rethrows an unexpected failure unchanged`() {
        val failure = IllegalStateException("repository unavailable")
        every { execution.currentActivityId } returns "validateApplication"
        every { useCase.validate(applicationId) } throws failure

        assertThatThrownBy { underTest.execute(execution) }.isSameAs(failure)
    }
}
