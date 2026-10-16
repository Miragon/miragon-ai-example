package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Records the back office's credit-rating decision and completes the "Check credit rating" task. */
interface CompleteCreditCheckUseCase {
    fun completeCreditCheck(id: ApplicationId, solvent: Boolean)
}
