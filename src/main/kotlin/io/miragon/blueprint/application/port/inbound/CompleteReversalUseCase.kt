package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** The back office confirms that a withdrawn application was reversed by hand. */
interface CompleteReversalUseCase {
    fun completeReversal(id: ApplicationId)
}
