package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.LeasingApplication

/**
 * Outbound port for customer-facing communication (contract, rejection). The example ships a logging
 * implementation; a real service would send an email or push message.
 */
interface NotificationPort {
    fun send(subject: String, application: LeasingApplication)
}
