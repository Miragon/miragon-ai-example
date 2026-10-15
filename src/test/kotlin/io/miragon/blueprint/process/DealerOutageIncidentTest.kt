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
import io.miragon.blueprint.process.util.failJobUntilRetriesAreExhausted
import io.miragon.blueprint.process.util.findProcessInstance
import io.miragon.blueprint.process.util.hasPassedInOrder
import io.miragon.bpmn.runtime.path.ProcessPath
import io.miragon.bpmn.runtime.path.then
import org.assertj.core.api.Assertions
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.runtime.Incident
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * The incident demo with the **real** use cases and the simulated dealer: ordering the poison bike
 * `BIKE-FAIL` fails the dealer call as an outage. `serviceTask_orderBike` runs as its own job
 * (asyncBefore) with the retry cycle `R3/PT10S`, so the engine tries the order three times and then
 * raises a `failedJob` incident on that task. The test fires the retries by hand instead of waiting
 * 10 seconds between them — the job executor is off in tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class DealerOutageIncidentTest {

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
    fun `dealer outage - the order job exhausts its retries and raises an incident on the order task`() {
        val application =
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = CustomerName("Ivan Incident"),
                email = Email("ivan@example.com"),
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = BikeId("BIKE-FAIL"),
                createdAt = LocalDateTime.now(),
            )
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)

        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendContract)
        process.correlateContractSigned(application.id)
        processEngine.executeJobFor(FlowNodes.EventContractSigned) // -> the order step's own job

        val failures = processEngine.failJobUntilRetriesAreExhausted(FlowNodes.ServiceTaskOrderBike)

        // R3: three attempts, each one failing on the simulated dealer outage
        Assertions.assertThat(failures).hasSize(3)
        failures.forEach { failure ->
            Assertions.assertThat(failure).hasStackTraceContaining("Bike dealer API unavailable for BIKE-FAIL")
        }

        val incident: Incident? =
            runtimeService
                .createIncidentQuery()
                .processInstanceId(instance.id)
                .singleResult()
        Assertions.assertThat(incident).isNotNull
        Assertions.assertThat(incident?.incidentType).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE)
        Assertions.assertThat(incident?.activityId).isEqualTo(FlowNodes.ServiceTaskOrderBike.ELEMENT_ID)

        // the instance is stuck before the order step; nothing of the failed order reached the application
        assertThat(instance)
            .isNotEnded
            .hasPassedInOrder(
                ProcessPath.from(FlowNodes.StartEventLeasingRequestReceived)
                    .then { it.businessRuleTaskCheckCreditRating }
                    .then { it.gatewayIsSolvent }
                    .then { it.serviceTaskSendContract }
                    .then { it.gatewayAwaitSignature }
                    .then { it.eventContractSigned }
                    .then { it.gatewayBikeSourceJoin },
            )
            .hasNotPassed(
                FlowNodes.ServiceTaskOrderBike.ELEMENT_ID,
                FlowNodes.EventBikeUnavailable.ELEMENT_ID,
            )
        val stored = repository.findById(application.id)
        Assertions.assertThat(stored?.status).isEqualTo(LeasingStatus.RECEIVED)
        Assertions.assertThat(stored?.orderId).isNull()
    }
}
