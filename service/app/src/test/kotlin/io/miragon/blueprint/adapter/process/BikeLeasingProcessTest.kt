package io.miragon.blueprint.adapter.process

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase
import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.miragon.bpmn.runtime.path.ProcessPath
import io.miragon.bpmn.runtime.path.interruptedBy
import io.miragon.bpmn.runtime.path.then
import io.mockk.every
import io.mockk.verify
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
 * Walks the bike-leasing process through the embedded engine with every use case behind a delegate
 * mocked, so each test pins the path the model takes and which business steps it triggers. The job
 * executor is off in tests: every async continuation is driven by hand.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeLeasingProcessTest {

    @Autowired
    private lateinit var process: LeasingProcess

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @MockkBean(relaxed = true)
    private lateinit var validateApplicationUseCase: ValidateApplicationUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendContractUseCase: SendContractUseCase

    @MockkBean(relaxed = true)
    private lateinit var issueInsurancePolicyUseCase: IssueInsurancePolicyUseCase

    @MockkBean(relaxed = true)
    private lateinit var orderBikeUseCase: OrderBikeUseCase

    @MockkBean(relaxed = true)
    private lateinit var rejectApplicationUseCase: RejectApplicationUseCase

    @BeforeEach
    fun setUp() {
        init(processEngine)
        every { orderBikeUseCase.orderBike(any()) } returns OrderId("ORDER-1")
    }

    @Test
    fun `happy path - solvent, signed, insured, ordered and handed over`() {
        val id = submitUntilCreditCheck()
        val instance = runtimeService.findProcessInstance(id)

        process.completeCreditCheck(id, solvent = true)
        processEngine.executeJobFor(FlowNodes.UserTaskCheckCreditRating)

        process.correlateContractSigned(id)
        processEngine.executeJobFor(FlowNodes.ReceiveTaskContractSigned)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike)

        process.correlateHandoverReported(id)
        processEngine.executeJobFor(FlowNodes.EventHandoverReported)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilContractSigned()
                    .then { it.gatewayFork }
                    .then { it.serviceTaskIssueInsurancePolicy }
                    .then { it.gatewayJoin },
            )
            .hasPassedInOrder(
                pathUntilContractSigned()
                    .then { it.gatewayFork }
                    .then { it.serviceTaskOrderBike }
                    .then { it.gatewayJoin }
                    .then { it.eventHandoverReported }
                    .then { it.endEventLeasingActive },
            )
            .hasNotPassed(
                FlowNodes.ServiceTaskSendRejection.ELEMENT_ID,
                FlowNodes.SubProcessApplicationWithdrawn.ELEMENT_ID,
            )

        verify(exactly = 1) { validateApplicationUseCase.validate(id) }
        verify(exactly = 1) { sendContractUseCase.sendContract(id) }
        verify(exactly = 1) { issueInsurancePolicyUseCase.issuePolicy(id) }
        verify(exactly = 1) { orderBikeUseCase.orderBike(id) }
        verify(exactly = 0) { rejectApplicationUseCase.reject(any()) }
    }

    @Test
    fun `invalid application - the validation error routes straight to the rejection`() {
        every { validateApplicationUseCase.validate(any()) } throws
            ApplicationInvalidException(ApplicationId.new(), listOf("the applicant must be at least 18 years old"))
        val id = submit()
        val instance = runtimeService.findProcessInstance(id)

        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendRejection)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                ProcessPath.from(FlowNodes.StartEventLeasingRequestReceived)
                    .then { it.serviceTaskValidateApplication }
                    .interruptedBy(FlowNodes.ServiceTaskValidateApplication) { it.eventApplicationInvalid }
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
    }

    @Test
    fun `not solvent - the credit check routes the application to the rejection`() {
        val id = submitUntilCreditCheck()
        val instance = runtimeService.findProcessInstance(id)

        process.completeCreditCheck(id, solvent = false)
        processEngine.executeJobFor(FlowNodes.UserTaskCheckCreditRating)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendRejection)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilCreditChecked()
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(FlowNodes.ServiceTaskSendContract.ELEMENT_ID)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
        verify(exactly = 0) { sendContractUseCase.sendContract(any()) }
    }

    @Test
    fun `withdrawal during the credit check - the back office reverses the application`() {
        val id = submitUntilCreditCheck()
        val instance = runtimeService.findProcessInstance(id)

        process.correlateApplicationWithdrawn(id)
        processEngine.executeJobFor(FlowNodes.StartEventApplicationWithdrawn)
        assertThat(instance).isWaitingAt(FlowNodes.UserTaskReverseApplication.ELEMENT_ID)

        process.completeReversal(id)
        processEngine.executeJobFor(FlowNodes.UserTaskReverseApplication)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(pathOfTheReversal())
            .hasNotPassed(
                FlowNodes.GatewayIsSolvent.ELEMENT_ID,
                FlowNodes.ServiceTaskSendRejection.ELEMENT_ID,
            )
        verify(exactly = 0) { sendContractUseCase.sendContract(any()) }
    }

    @Test
    fun `withdrawal while the signature is awaited - the contract is never signed`() {
        val id = submitUntilCreditCheck()
        val instance = runtimeService.findProcessInstance(id)
        process.completeCreditCheck(id, solvent = true)
        processEngine.executeJobFor(FlowNodes.UserTaskCheckCreditRating)
        assertThat(instance).isWaitingAt(FlowNodes.ReceiveTaskContractSigned.ELEMENT_ID)

        process.correlateApplicationWithdrawn(id)
        processEngine.executeJobFor(FlowNodes.StartEventApplicationWithdrawn)
        process.completeReversal(id)
        processEngine.executeJobFor(FlowNodes.UserTaskReverseApplication)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(pathOfTheReversal())
            .hasNotPassed(
                FlowNodes.GatewayFork.ELEMENT_ID,
                FlowNodes.ServiceTaskOrderBike.ELEMENT_ID,
            )
        verify(exactly = 1) { sendContractUseCase.sendContract(id) }
        verify(exactly = 0) { orderBikeUseCase.orderBike(any()) }
    }

    @Test
    fun `withdrawal after the order - the back office reverses what was already done`() {
        val id = submitUntilCreditCheck()
        val instance = runtimeService.findProcessInstance(id)
        process.completeCreditCheck(id, solvent = true)
        processEngine.executeJobFor(FlowNodes.UserTaskCheckCreditRating)
        process.correlateContractSigned(id)
        processEngine.executeJobFor(FlowNodes.ReceiveTaskContractSigned)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike)
        assertThat(instance).isWaitingAt(FlowNodes.EventHandoverReported.ELEMENT_ID)

        process.correlateApplicationWithdrawn(id)
        processEngine.executeJobFor(FlowNodes.StartEventApplicationWithdrawn)
        process.completeReversal(id)
        processEngine.executeJobFor(FlowNodes.UserTaskReverseApplication)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(pathOfTheReversal())
            .hasNotPassed(FlowNodes.EndEventLeasingActive.ELEMENT_ID)
        verify(exactly = 1) { issueInsurancePolicyUseCase.issuePolicy(id) }
        verify(exactly = 1) { orderBikeUseCase.orderBike(id) }
    }

    private fun submitUntilCreditCheck(): ApplicationId {
        val id = submit()
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskValidateApplication)
        assertThat(runtimeService.findProcessInstance(id)).isWaitingAt(FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID)
        return id
    }

    private fun pathUntilCreditChecked() =
        ProcessPath.from(FlowNodes.StartEventLeasingRequestReceived)
            .then { it.serviceTaskValidateApplication }
            .then { it.userTaskCheckCreditRating }
            .then { it.gatewayIsSolvent }

    private fun pathUntilContractSigned() =
        pathUntilCreditChecked()
            .then { it.serviceTaskSendContract }
            .then { it.receiveTaskContractSigned }

    private fun pathOfTheReversal() =
        ProcessPath.from(FlowNodes.StartEventApplicationWithdrawn)
            .then { it.userTaskReverseApplication }
            .then { it.endEventApplicationCancelled }

    private fun submit(): ApplicationId {
        val application = testLeasingApplication(id = ApplicationId.new())
        process.submitRequest(application)
        return application.id
    }
}
