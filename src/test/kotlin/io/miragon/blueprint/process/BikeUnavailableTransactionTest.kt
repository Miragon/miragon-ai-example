package io.miragon.blueprint.process

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.process.util.executeJobFor
import io.miragon.blueprint.process.util.findProcessInstance
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
import java.time.LocalDateTime

/**
 * Runs the out-of-stock order with the **real** use cases instead of mocks. The embedded engine and
 * the `@Transactional` services share one transaction, so the `BikeUnavailableException` must not
 * mark it rollback-only — otherwise the `bikeUnavailable` BPMN error could never be committed and
 * the order job would fail into an incident. [BikeLeasingProcessTest] mocks the use cases and
 * therefore cannot see this.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeUnavailableTransactionTest {

    @Autowired
    private lateinit var process: LeasingProcess

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
    fun `bike unavailable - the BPMN error raised by the real order service is committed`() {
        // BIKE-OOS is on the simulated dealer's out-of-stock list
        val application = receive(bikeId = BikeId("BIKE-OOS"))
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)

        driveUntilTheOrderStepRuns(application.id) // unavailable -> BPMN error -> clarify alternative

        assertThat(instance)
            .isWaitingAt(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)
            .hasPassed(FlowNodes.EventBikeUnavailable.ELEMENT_ID)
    }

    @Test
    fun `bike unavailable - the bike the process carries is stored although the order service throws`() {
        // the stored application names another bike than the out-of-stock one the process carries
        val application = receive(bikeId = BikeId("BIKE-900"))
        repository.save(application)
        process.submitRequest(application.selectAlternative(BikeId("BIKE-OOS")))

        driveUntilTheOrderStepRuns(application.id) // unavailable -> BPMN error -> clarify alternative

        Assertions.assertThat(repository.findById(application.id)?.bikeId).isEqualTo(BikeId("BIKE-OOS"))

        // the alternative reaches the application through the process variable alone
        process.completeAlternativeClarification(application.id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"))
        processEngine.executeJobFor(FlowNodes.UserTaskClarifyAlternative)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike) // the re-order succeeds

        val reordered = repository.findById(application.id)
        Assertions.assertThat(reordered?.bikeId).isEqualTo(BikeId("BIKE-ALT"))
        Assertions.assertThat(reordered?.status).isEqualTo(LeasingStatus.ORDERED)
        Assertions.assertThat(reordered?.orderId).isNotNull
    }

    @Test
    fun `bike unavailable - declining the alternative revokes the contract and cancels the application`() {
        val application = receive(bikeId = BikeId("BIKE-OOS"))
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)
        driveUntilTheOrderStepRuns(application.id) // unavailable -> BPMN error -> clarify alternative

        process.completeAlternativeClarification(application.id, alternativeFound = false)
        // the compensation handler runs inside the throw event's continuation, so one job takes the
        // case from the declined alternative through "Cancel contract" to the cancellation confirmation
        processEngine.executeJobFor(FlowNodes.UserTaskClarifyAlternative)
        processEngine.executeJobFor(FlowNodes.ServiceTaskConfirmContractCancellation) // -> contract cancelled

        assertThat(instance)
            .isEnded
            .hasPassed(
                FlowNodes.ServiceTaskCancelContract.ELEMENT_ID,
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
            )
        val cancelled = repository.findById(application.id)
        Assertions.assertThat(cancelled?.status).isEqualTo(LeasingStatus.CANCELLED)
        // the contract that "Cancel contract" revoked was issued and recorded by "Send contract"
        Assertions.assertThat(cancelled?.contractId).isNotNull
        Assertions.assertThat(cancelled?.orderId).isNull()
    }

    /** Async continuations up to and including the order step: credit check, contract sent and signed. */
    private fun driveUntilTheOrderStepRuns(id: ApplicationId) {
        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendContract)
        process.correlateContractSigned(id)
        processEngine.executeJobFor(FlowNodes.EventContractSigned)
        processEngine.executeJobFor(FlowNodes.ServiceTaskOrderBike)
    }

    private fun receive(bikeId: BikeId) =
        LeasingApplication.receive(
            id = ApplicationId.new(),
            customerName = CustomerName("Test Customer"),
            email = Email("test@example.com"),
            age = 35,
            monthlyNetIncome = 3500.0,
            bikeId = bikeId,
            createdAt = LocalDateTime.now(),
        )
}
