package io.miragon.blueprint.adapter.outbound.contract

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Example contract adapter — logs instead of calling a real contract system. Swap this for a real
 * integration without touching the application layer.
 */
@Component
class LoggingContractAdapter : ContractPort {

    private val log = KotlinLogging.logger {}

    override fun issueContract(id: ApplicationId): ContractId {
        val contractId = ContractId("CONTRACT-${UUID.randomUUID()}")
        log.info { "Issued contract ${contractId.value} for application ${id.value}" }
        return contractId
    }
}
