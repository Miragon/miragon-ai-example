package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Checks the business rules of a received application; throws when it is invalid. */
fun interface ValidateApplicationUseCase {
    fun validate(id: ApplicationId)
}
