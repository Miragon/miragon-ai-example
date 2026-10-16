package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.CompleteCreditCheckUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompleteCreditCheckService(
    private val repository: LeasingApplicationRepository,
    private val process: LeasingProcess,
) : CompleteCreditCheckUseCase {

    override fun completeCreditCheck(id: ApplicationId, solvent: Boolean) {
        repository.findById(id) ?: throw ApplicationNotFoundException(id)
        process.completeCreditCheck(id, solvent)
    }
}
