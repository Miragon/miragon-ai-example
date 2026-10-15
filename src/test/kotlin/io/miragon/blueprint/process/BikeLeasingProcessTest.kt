package io.miragon.blueprint.process

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase
import io.miragon.blueprint.application.port.inbound.CancelContractUseCase
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase
import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase
import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.BikeUnavailableException
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.process.util.executeJobFor
import io.miragon.blueprint.process.util.findProcessInstance
import io.miragon.blueprint.process.util.fireTimer
import io.miragon.blueprint.process.util.hasPassed
import io.miragon.blueprint.process.util.hasPassedInOrder
import io.miragon.bpmn.runtime.path.ProcessPath
import io.miragon.bpmn.runtime.path.interruptedBy
import io.miragon.bpmn.runtime.path.then
import io.miragon.bpmn.runtime.path.throwingCompensation
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * Walks the core bike-leasing process through the embedded engine with every use case behind a
 * delegate mocked, so each test pins the path the model takes and which business steps it triggers.
 * The job executor is off in tests: every async continuation and timer is driven by hand.
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
    private lateinit var rejectApplicationUseCase: RejectApplicationUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendContractUseCase: SendContractUseCase

    @MockkBean(relaxed = true)
    private lateinit var cancelContractUseCase: CancelContractUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendCancellationConfirmationUseCase: SendCancellationConfirmationUseCase

    @MockkBean(relaxed = true)
    private lateinit var orderBikeUseCase: OrderBikeUseCase

    @MockkBean(relaxed = true)
    private lateinit var activateLeasingUseCase: ActivateLeasingUseCase

    @BeforeEach
    fun setUp() {
        init(processEngine)
        every { orderBikeUseCase.orderBike(any(), any()) } returns OrderId("ORDER-1")
    }

    @Test
    fun `happy path - contract signed, bike available, leasing becomes active`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        // async continuations up to the contract-signature wait state
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendContract)

        process.correlateContractSigned(id)
        processEngine.executeJobFor(FlowNodes.EventContractSigned) // -> the order step's own job
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike) // -> handover wait state

        process.correlateHandoverReported(id)
        processEngine.executeJobFor(FlowNodes.EventHandoverReported) // -> withdrawal-period timer

        processEngine.fireTimer(FlowNodes.EventWithdrawalPeriodElapsed)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilContractSigned()
                    .then { it.gatewayBikeSourceJoin }
                    .then { it.serviceTaskOrderBike }
                    .then { it.eventHandoverReported }
                    .then { it.eventWithdrawalPeriodElapsed }
                    .then { it.endEventLeasingActive },
            )
            .hasNotPassed(
                FlowNodes.EventSignatureDeadline.ELEMENT_ID,
                FlowNodes.EventBikeUnavailable.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
                FlowNodes.ServiceTaskCancelContract.ELEMENT_ID,
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
            )

        verify(exactly = 1) { sendContractUseCase.sendContract(id) }
        verify(exactly = 1) { orderBikeUseCase.orderBike(id, BikeId("BIKE-TEST")) }
        verify(exactly = 1) { activateLeasingUseCase.activate(id) }
        verify(exactly = 0) { rejectApplicationUseCase.reject(any()) }
        verify(exactly = 0) { cancelContractUseCase.cancelContract(any()) }
    }

    @Test
    fun `signature deadline - a contract not signed within 14 days is rejected`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        // async continuations up to the contract-signature wait state
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendContract)

        processEngine.fireTimer(FlowNodes.EventSignatureDeadline) // 14 days -> rejection reason -> rejection
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendRejection)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilSignatureAwaited()
                    .then { it.eventSignatureDeadline }
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(
                FlowNodes.EventContractSigned.ELEMENT_ID,
                FlowNodes.ServiceTaskOrderBike.ELEMENT_ID,
                FlowNodes.EndEventLeasingActive.ELEMENT_ID,
            )

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
        verify(exactly = 0) { orderBikeUseCase.orderBike(any(), any()) }
        // the rejection does not compensate: the contract that was sent stays as it is
        verify(exactly = 0) { cancelContractUseCase.cancelContract(any()) }
    }

    @Test
    fun `not solvent - the DMN routes the application straight to rejection`() {
        // age below 18 cannot sign a leasing contract, so the DMN returns solvent = false
        val id = submit(age = 15, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived) // -> DMN -> not solvent -> rejection
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendRejection)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilCreditRatingChecked()
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(FlowNodes.ServiceTaskSendContract.ELEMENT_ID)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
        verify(exactly = 0) { sendContractUseCase.sendContract(any()) }
    }

    @Test
    fun `bike unavailable - clarifying an alternative re-orders and leasing becomes active`() {
        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)
        assertThat(instance).isWaitingAt(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)

        // the alternative is clarified from the outside — the "external" completion of the user task
        process.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"))
        assertThat(instance).variables().containsEntry(FlowNodes.UserTaskClarifyAlternative.Variables.BIKE_ID.value, "BIKE-ALT")
        processEngine.executeJobFor(FlowNodes.UserTaskClarifyAlternative) // -> back to the order step's job
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike) // re-order succeeds -> handover wait state

        process.correlateHandoverReported(id)
        processEngine.executeJobFor(FlowNodes.EventHandoverReported)
        processEngine.fireTimer(FlowNodes.EventWithdrawalPeriodElapsed)

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilAlternativeClarified()
                    .then { it.gatewayBikeSourceJoin }
                    .then { it.serviceTaskOrderBike }
                    .then { it.eventHandoverReported }
                    .then { it.eventWithdrawalPeriodElapsed }
                    .then { it.endEventLeasingActive },
            )
            .hasNotPassed(
                FlowNodes.EventTriggerReversal.ELEMENT_ID,
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
            )

        verifyOrder {
            orderBikeUseCase.orderBike(id, BikeId("BIKE-TEST"))
            orderBikeUseCase.orderBike(id, BikeId("BIKE-ALT"))
        }
        verify(exactly = 1) { activateLeasingUseCase.activate(id) }
        verify(exactly = 0) { cancelContractUseCase.cancelContract(any()) }
    }

    @Test
    fun `bike unavailable - declining the alternative cancels the contract`() {
        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)

        process.completeAlternativeClarification(id, alternativeFound = false)
        // the compensation handler runs inside the throw event's continuation, so one job takes the
        // case from the declined alternative through "Cancel contract" to the cancellation confirmation
        processEngine.executeJobFor(FlowNodes.UserTaskClarifyAlternative)
        processEngine.executeJobFor(FlowNodes.ServiceTaskConfirmContractCancellation) // -> contract cancelled

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(pathUntilAlternativeClarified())
            // the compensation handler finishes before the throw event that triggered it, so this part
            // of the walk is asserted without an order
            .hasPassed(
                ProcessPath.from(FlowNodes.GatewayAlternativeFound)
                    .then { it.eventTriggerReversal }
                    .throwingCompensation(FlowNodes.EventCompensateContract) { it.serviceTaskCancelContract }
                    .then { it.serviceTaskConfirmContractCancellation }
                    .then { it.endEventContractCancelled },
            )
            .hasNotPassed(
                FlowNodes.EventHandoverReported.ELEMENT_ID,
                FlowNodes.EndEventLeasingActive.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
            )

        verify(exactly = 1) { orderBikeUseCase.orderBike(id, BikeId("BIKE-TEST")) }
        verify(exactly = 1) { cancelContractUseCase.cancelContract(id) }
        verify(exactly = 1) { sendCancellationConfirmationUseCase.sendCancellationConfirmation(id) }
        verify(exactly = 0) { activateLeasingUseCase.activate(any()) }
    }

    /** Drives a signed application to the clarify-alternative task: the dealer has the bike out of stock. */
    private fun submitUntilBikeUnavailable(): ApplicationId {
        // the first order finds the requested bike unavailable, a re-order after an alternative succeeds
        every { orderBikeUseCase.orderBike(any(), any()) } throws
            BikeUnavailableException(BikeId("BIKE-TEST")) andThen OrderId("ORDER-2")

        val id = submit(age = 35, income = 3500.0)
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendContract)
        process.correlateContractSigned(id)
        processEngine.executeJobFor(FlowNodes.EventContractSigned)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike) // unavailable -> parks on the clarify-alternative user task
        return id
    }

    private fun pathUntilCreditRatingChecked() =
        ProcessPath.from(FlowNodes.StartEventLeasingRequestReceived)
            .then { it.businessRuleTaskCheckCreditRating }
            .then { it.gatewayIsSolvent }

    private fun pathUntilSignatureAwaited() =
        pathUntilCreditRatingChecked()
            .then { it.serviceTaskSendContract }
            .then { it.gatewayAwaitSignature }

    private fun pathUntilContractSigned() =
        pathUntilSignatureAwaited()
            .then { it.eventContractSigned }

    private fun pathUntilAlternativeClarified() =
        pathUntilContractSigned()
            .then { it.gatewayBikeSourceJoin }
            .then { it.serviceTaskOrderBike }
            .interruptedBy(FlowNodes.ServiceTaskOrderBike) { it.eventBikeUnavailable }
            .then { it.userTaskClarifyAlternative }
            .then { it.gatewayAlternativeFound }

    private fun submit(age: Int, income: Double, bikeId: String = "BIKE-TEST"): ApplicationId {
        val application =
            LeasingApplication(
                id = ApplicationId.new(),
                customerName = CustomerName("Test Customer"),
                email = Email("test@example.com"),
                age = age,
                monthlyNetIncome = income,
                bikeId = BikeId(bikeId),
                status = LeasingStatus.RECEIVED,
                createdAt = LocalDateTime.now(),
            )
        process.submitRequest(application)
        return application.id
    }
}
