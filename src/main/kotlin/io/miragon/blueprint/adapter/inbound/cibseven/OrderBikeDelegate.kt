package io.miragon.blueprint.adapter.inbound.cibseven

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.cibseven.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component

@Component
class OrderBikeDelegate(
    private val useCase: OrderBikeUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        useCase.orderBike(ApplicationId.of(execution.processBusinessKey))
    }
}
