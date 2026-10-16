package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SignContractUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SignContractService(
    private val process: LeasingProcess,
    private val repository: LeasingApplicationRepository,
) : SignContractUseCase {

    override fun signContract(id: ApplicationId) {
        val application = repository.findById(id) ?: throw ApplicationNotFoundException(id)
        val signed = application.signContract()
        process.correlateContractSigned(id)
        repository.save(signed)
    }
}
