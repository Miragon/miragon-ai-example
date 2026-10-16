package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Issues the insurance policy that accompanies the leasing contract. */
fun interface IssueInsurancePolicyUseCase {
    fun issuePolicy(id: ApplicationId)
}
