package io.miragon.blueprint.adapter.process

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.inbound.CompleteCreditCheckUseCase
import io.miragon.blueprint.application.port.inbound.SignContractUseCase
import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingStatus
import org.assertj.core.api.Assertions
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * With the **real** services: the moment a status offers the customer an action, the process already
 * waits for it. The frontend shows "sign" for CONTRACT_SENT and "report handover" for ORDERED, so a
 * status committed in an earlier transaction than the wait state would answer the click with a 409.
 */
@SpringBootTest
@ActiveProfiles("test")
class StatusMatchesWaitStateTest {

    @Autowired
    private lateinit var submitLeasingRequest: SubmitLeasingRequestUseCase

    @Autowired
    private lateinit var completeCreditCheck: CompleteCreditCheckUseCase

    @Autowired
    private lateinit var signContract: SignContractUseCase

    @Autowired
    private lateinit var repository: LeasingApplicationRepository

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @BeforeEach
    fun setUp() {
        init(processEngine)
    }

    @Test
    fun `CONTRACT_SENT and ORDERED are committed together with the wait state they announce`() {
        val id = submit()
        val instance = runtimeService.findProcessInstance(id)
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskValidateApplication)

        completeCreditCheck.completeCreditCheck(id, solvent = true)
        processEngine.executeJobFor(FlowNodes.UserTaskCheckCreditRating)

        Assertions.assertThat(statusOf(id)).isEqualTo(LeasingStatus.CONTRACT_SENT)
        assertThat(instance).isWaitingAt(FlowNodes.EventContractSigned.ELEMENT_ID)

        signContract.signContract(id)
        processEngine.executeJobFor(FlowNodes.EventContractSigned)
        Assertions.assertThat(statusOf(id)).isEqualTo(LeasingStatus.CONTRACT_SIGNED)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike)

        Assertions.assertThat(statusOf(id)).isEqualTo(LeasingStatus.ORDERED)
        assertThat(instance).isWaitingAt(FlowNodes.EventHandoverReported.ELEMENT_ID)
    }

    private fun statusOf(id: ApplicationId) = repository.findById(id)?.status

    private fun submit(): ApplicationId =
        submitLeasingRequest.submit(
            SubmitLeasingRequestUseCase.Command(
                customerName = CustomerName("Wanda Wait"),
                email = Email("wanda@example.com"),
                age = 30,
                monthlyNetIncome = 3000.0,
                bikeId = BikeId("BIKE-800"),
            ),
        )
}
