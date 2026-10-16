package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetLeasingApplicationService(
    private val repository: LeasingApplicationRepository,
) : GetLeasingApplicationQuery {

    override fun byId(id: ApplicationId): LeasingApplication? = repository.findById(id)
}
