package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication

/** Persistence port for the leasing application aggregate. */
interface LeasingApplicationRepository {
    fun save(application: LeasingApplication): LeasingApplication

    fun findById(id: ApplicationId): LeasingApplication?
}
