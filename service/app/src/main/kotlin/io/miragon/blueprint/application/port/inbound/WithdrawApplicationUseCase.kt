package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** The customer withdraws a running application. */
interface WithdrawApplicationUseCase {
    fun withdraw(id: ApplicationId)
}
