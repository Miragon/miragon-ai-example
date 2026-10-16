package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId

/**
 * Outbound port to the (external) contract system that issues the leasing contract. Notifying the
 * customer to sign is a separate [NotificationPort] concern. A real integration replaces the logging
 * adapter without touching the application layer.
 */
fun interface ContractPort {
    fun issueContract(id: ApplicationId): ContractId
}
