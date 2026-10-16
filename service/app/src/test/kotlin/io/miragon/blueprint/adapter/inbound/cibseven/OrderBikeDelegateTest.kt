package io.miragon.blueprint.adapter.inbound.cibseven

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.junit.jupiter.api.Test

class OrderBikeDelegateTest {

    private val useCase = mockk<OrderBikeUseCase>()
    private val execution = mockk<DelegateExecution>()
    private val underTest = OrderBikeDelegate(useCase)

    @Test
    fun `orders the bike for the application of the business key`() {
        val applicationId = ApplicationId.new()
        every { execution.processBusinessKey } returns applicationId.value.toString()
        every { useCase.orderBike(applicationId) } returns OrderId("order-1")

        underTest.execute(execution)

        verify { useCase.orderBike(applicationId) }
        confirmVerified(useCase)
    }
}
