package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication

/** Reads a single leasing application — the domain view of one case. */
interface GetLeasingApplicationQuery {
    fun byId(id: ApplicationId): LeasingApplication?
}
