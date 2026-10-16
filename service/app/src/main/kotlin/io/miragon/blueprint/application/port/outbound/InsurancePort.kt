package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.PolicyId

/**
 * Outbound port to the (external) insurer that issues the policy for the leased bike. A real
 * integration replaces the logging adapter without touching the application layer.
 */
fun interface InsurancePort {
    fun issuePolicy(id: ApplicationId): PolicyId
}
