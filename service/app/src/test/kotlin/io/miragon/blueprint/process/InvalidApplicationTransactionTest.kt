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
 * The validation with the **real** services: an invalid application raises a domain exception inside
 * the engine's job transaction. The delegate turns it into the BPMN error `applicationInvalid`, so the
 * transaction must still commit and the process must reach the rejection instead of an incident.
 */
@SpringBootTest
@ActiveProfiles("test")
class InvalidApplicationTransactionTest {

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
    fun `an under-age applicant is rejected, not stuck in an incident`() {
        val application =
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = CustomerName("Tim Teen"),
                email = Email("tim@example.com"),
                age = 17,
                monthlyNetIncome = 900.0,
                bikeId = BikeId("BIKE-900"),
                bikeModel = "Gravel Explorer 900",
                createdAt = LocalDateTime.now(),
            )
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)

        processEngine.executeJobFor(FlowNodes.StartEventLeasingRequestReceived)
        processEngine.executeJobFor(FlowNodes.ServiceTaskSendRejection)

        assertThat(instance).isEnded.hasPassed(FlowNodes.EndEventApplicationRejected.ELEMENT_ID)
        Assertions.assertThat(repository.findById(application.id)?.status).isEqualTo(LeasingStatus.REJECTED)
    }
}
