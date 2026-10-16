package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase
import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class IssueInsurancePolicyService(
    private val repository: LeasingApplicationRepository,
    private val insurance: InsurancePort,
) : IssueInsurancePolicyUseCase {

    override fun issuePolicy(id: ApplicationId) {
        val application = repository.findById(id) ?: throw ApplicationNotFoundException(id)
        val policyId = insurance.issuePolicy(id)
        repository.save(application.documentPolicy(policyId))
    }
}
