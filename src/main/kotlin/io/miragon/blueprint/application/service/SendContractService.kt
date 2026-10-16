package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SendContractService(
    private val repository: LeasingApplicationRepository,
    private val contract: ContractPort,
    private val notification: NotificationPort,
) : SendContractUseCase {

    override fun sendContract(id: ApplicationId) {
        val application = repository.findById(id) ?: throw ApplicationNotFoundException(id)
        val contractId = contract.issueContract(id)
        repository.save(application.sendContract(contractId))
        notification.send("Please review and sign your leasing contract", application)
    }
}
