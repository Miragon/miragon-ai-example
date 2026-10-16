package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.PolicyId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class IssueInsurancePolicyServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val insurance = mockk<InsurancePort>()
    private val underTest = IssueInsurancePolicyService(repository = repository, insurance = insurance)

    @Test
    fun `issuePolicy issues the policy and records it without touching the status`() {
        val application = testLeasingApplication(status = LeasingStatus.ORDERED)
        every { repository.findById(application.id) } returns application
        every { insurance.issuePolicy(application.id) } returns PolicyId("POLICY-1")
        every { repository.save(any()) } answers { firstArg() }

        underTest.issuePolicy(application.id)

        verify {
            repository.save(match { it.policyId == PolicyId("POLICY-1") && it.status == LeasingStatus.ORDERED })
        }
    }
}
