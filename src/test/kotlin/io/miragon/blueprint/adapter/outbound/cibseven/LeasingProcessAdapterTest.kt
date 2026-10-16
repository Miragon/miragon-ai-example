package io.miragon.blueprint.adapter.outbound.cibseven

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.adapter.process.Messages
import io.miragon.blueprint.domain.leasing.ActionNotAvailableException
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.BackOfficeTaskType
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.cibseven.bpm.engine.runtime.MessageCorrelationBuilder
import org.cibseven.bpm.engine.runtime.ProcessInstance
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery
import org.cibseven.bpm.engine.task.Task
import org.cibseven.bpm.engine.task.TaskQuery
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class LeasingProcessAdapterTest {

    private val runtimeService = mockk<RuntimeService>()
    private val taskService = mockk<TaskService>()
    private val underTest = LeasingProcessAdapter(runtimeService = runtimeService, taskService = taskService)

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `submitRequest starts the process by message with the start variables`() {

        // given: a leasing application and a stubbed runtime service
        val application = testLeasingApplication(id = id)
        every { runtimeService.startProcessInstanceByMessage(any(), any<String>(), any()) } returns mockk()

        // when: the request is submitted
        underTest.submitRequest(application)

        // then: the leasing-request message starts the process, keyed by the application id
        verify {
            runtimeService.startProcessInstanceByMessage(
                Messages.MIRAVELO_LEASING_REQUEST_RECEIVED.value,
                id.value.toString(),
                mapOf(
                    "applicationId" to id.value.toString(),
                    "bikeId" to "BIKE-900",
                    "bikeModel" to "Gravel Explorer 900",
                ),
            )
        }
        confirmVerified(runtimeService)
    }

    @Test
    fun `completeCreditCheck completes the credit-check task with the decision`() {
        val query = openTaskQuery(task("task-1"))

        underTest.completeCreditCheck(id, solvent = true)

        verify { query.processInstanceBusinessKey(id.value.toString()) }
        verify { query.taskDefinitionKey(FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID) }
        verify { taskService.complete("task-1", mapOf("solvent" to true)) }
    }

    @Test
    fun `completeReversal completes the reversal task without variables`() {
        val query = openTaskQuery(task("task-2"))

        underTest.completeReversal(id)

        verify { query.taskDefinitionKey(FlowNodes.UserTaskReverseApplication.ELEMENT_ID) }
        verify { taskService.complete("task-2", emptyMap()) }
    }

    @Test
    fun `completing a task fails loudly when none is open`() {
        openTaskQuery(null)

        assertThatThrownBy { underTest.completeReversal(id) }
            .isInstanceOf(ActionNotAvailableException::class.java)
            .hasMessage("No open 'userTask_reverseApplication' task for business key ${id.value}")
        verify(exactly = 0) { taskService.complete(any(), any()) }
    }

    @Test
    fun `findOpenTasks maps both user tasks to back-office tasks keyed by the business key`() {

        // given: an open credit check and an open reversal of two instances
        val created = LocalDateTime.of(2024, 1, 15, 10, 30)
        val creditCheck = task("t1", "pi-1", FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID, created)
        val reversal = task("t2", "pi-2", FlowNodes.UserTaskReverseApplication.ELEMENT_ID, created)
        val otherId = ApplicationId.new()
        val taskQuery = mockk<TaskQuery>()
        every { taskService.createTaskQuery() } returns taskQuery
        every { taskQuery.taskDefinitionKeyIn(*anyVararg()) } returns taskQuery
        every { taskQuery.orderByTaskCreateTime() } returns taskQuery
        every { taskQuery.asc() } returns taskQuery
        every { taskQuery.list() } returns listOf(creditCheck, reversal)
        val instanceQuery = mockk<ProcessInstanceQuery>()
        every { runtimeService.createProcessInstanceQuery() } returns instanceQuery
        every { instanceQuery.processInstanceIds(setOf("pi-1", "pi-2")) } returns instanceQuery
        every { instanceQuery.list() } returns listOf(instance("pi-1", id), instance("pi-2", otherId))

        // when: the open tasks are read
        val tasks = underTest.findOpenTasks()

        // then: each task is typed and keyed by its application id
        assertThat(tasks).containsExactly(
            BackOfficeTask(id, BackOfficeTaskType.CREDIT_CHECK, created),
            BackOfficeTask(otherId, BackOfficeTaskType.REVERSAL, created),
        )
        verify {
            taskQuery.taskDefinitionKeyIn(
                FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID,
                FlowNodes.UserTaskReverseApplication.ELEMENT_ID,
            )
        }
    }

    @Test
    fun `findOpenTasks returns nothing when no task is open`() {
        val taskQuery = mockk<TaskQuery>()
        every { taskService.createTaskQuery() } returns taskQuery
        every { taskQuery.taskDefinitionKeyIn(*anyVararg()) } returns taskQuery
        every { taskQuery.orderByTaskCreateTime() } returns taskQuery
        every { taskQuery.asc() } returns taskQuery
        every { taskQuery.list() } returns emptyList()

        assertThat(underTest.findOpenTasks()).isEmpty()
        verify(exactly = 0) { runtimeService.createProcessInstanceQuery() }
    }

    @Test
    fun `correlateContractSigned correlates the message by business key`() {
        assertCorrelation(Messages.MIRAVELO_CONTRACT_SIGNED.value) { underTest.correlateContractSigned(id) }
    }

    @Test
    fun `correlateHandoverReported correlates the message by business key`() {
        assertCorrelation(Messages.MIRAVELO_HANDOVER_REPORTED.value) { underTest.correlateHandoverReported(id) }
    }

    @Test
    fun `correlateApplicationWithdrawn correlates the message by business key`() {
        assertCorrelation(Messages.MIRAVELO_APPLICATION_WITHDRAWN.value) { underTest.correlateApplicationWithdrawn(id) }
    }

    private fun task(
        taskId: String,
        processInstanceId: String = "pi",
        definitionKey: String = "",
        created: LocalDateTime = LocalDateTime.now(),
    ) = mockk<Task> {
        every { this@mockk.id } returns taskId
        every { this@mockk.processInstanceId } returns processInstanceId
        every { taskDefinitionKey } returns definitionKey
        every { createTime } returns Date.from(created.atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun instance(processInstanceId: String, applicationId: ApplicationId) =
        mockk<ProcessInstance> {
            every { id } returns processInstanceId
            every { businessKey } returns applicationId.value.toString()
        }

    private fun openTaskQuery(task: Task?): TaskQuery {
        val query = mockk<TaskQuery>()
        every { taskService.createTaskQuery() } returns query
        every { query.processInstanceBusinessKey(any()) } returns query
        every { query.taskDefinitionKey(any()) } returns query
        every { query.singleResult() } returns task
        every { taskService.complete(any(), any()) } returns Unit
        return query
    }

    private fun assertCorrelation(expectedMessage: String, action: () -> Unit) {
        val builder = mockk<MessageCorrelationBuilder>()
        every { runtimeService.createMessageCorrelation(any()) } returns builder
        every { builder.processInstanceBusinessKey(any()) } returns builder
        every { builder.correlate() } returns mockk()

        action()

        verify { runtimeService.createMessageCorrelation(expectedMessage) }
        verify { builder.processInstanceBusinessKey(id.value.toString()) }
        verify { builder.correlate() }
        confirmVerified(runtimeService, builder)
    }
}
