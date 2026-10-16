package io.miragon.blueprint.adapter.outbound.insurance

import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.PolicyId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Example insurance adapter — logs instead of calling a real insurer. Swap this for a real
 * integration without touching the application layer.
 */
@Component
class LoggingInsuranceAdapter : InsurancePort {

    private val log = KotlinLogging.logger {}

    override fun issuePolicy(id: ApplicationId): PolicyId {
        val policyId = PolicyId("POLICY-${UUID.randomUUID()}")
        log.info { "Issued insurance policy ${policyId.value} for application ${id.value}" }
        return policyId
    }
}
