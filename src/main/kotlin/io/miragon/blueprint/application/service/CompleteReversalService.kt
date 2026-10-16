package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.CompleteReversalUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompleteReversalService(
    private val repository: LeasingApplicationRepository,
    private val process: LeasingProcess,
) : CompleteReversalUseCase {

    override fun completeReversal(id: ApplicationId) {
        val application = repository.findById(id) ?: throw ApplicationNotFoundException(id)
        process.completeReversal(id)
        repository.save(application.cancel())
    }
}
