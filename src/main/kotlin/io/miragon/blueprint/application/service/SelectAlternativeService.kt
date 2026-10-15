package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SelectAlternativeService(
    private val repository: LeasingApplicationRepository,
    private val process: LeasingProcess,
) : SelectAlternativeUseCase {

    /**
     * Completes the clarification with the customer's decision. The chosen bike is not stored here:
     * it travels to the re-order as the `bikeId` process variable, and the order step persists it —
     * the same way a completion through the Tasklist form reaches the domain.
     */
    override fun selectAlternative(command: SelectAlternativeUseCase.Command) {
        repository.findById(command.applicationId) ?: error("Unknown application ${command.applicationId}")
        process.completeAlternativeClarification(
            id = command.applicationId,
            alternativeFound = command.alternativeFound,
            bikeId = command.bikeId,
        )
    }
}
