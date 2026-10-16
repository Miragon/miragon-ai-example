package io.miragon.blueprint.adapter.outbound.cibseven

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.adapter.process.Messages
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.BackOfficeTaskType
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.springframework.stereotype.Component
import java.time.ZoneId

/**
 * Drives the embedded CIB seven engine. The application id is used as the process business key, so
 * later messages and user-task lookups correlate to the right instance by business key. Element ids,
 * message and variable names come from the typed process API generated from `bike-leasing.bpmn`.
 */
@Component
class LeasingProcessAdapter(
    private val runtimeService: RuntimeService,
    private val taskService: TaskService,
) : LeasingProcess {

    private val taskTypes = mapOf(
        FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID to BackOfficeTaskType.CREDIT_CHECK,
        FlowNodes.UserTaskReverseApplication.ELEMENT_ID to BackOfficeTaskType.REVERSAL,
    )

    override fun submitRequest(application: LeasingApplication) {
        val start = FlowNodes.StartEventLeasingRequestReceived.Variables
        runtimeService.startProcessInstanceByMessage(
            Messages.MIRAVELO_LEASING_REQUEST_RECEIVED.value,
            application.id.value.toString(),
            mapOf(
                start.APPLICATION_ID.value to application.id.value.toString(),
                start.BIKE_ID.value to application.bikeId.value,
                start.BIKE_MODEL.value to application.bikeModel,
            ),
        )
    }

    override fun correlateContractSigned(id: ApplicationId) =
        runtimeService.correlateByBusinessKey(Messages.MIRAVELO_CONTRACT_SIGNED.value, id.value.toString())

    override fun correlateHandoverReported(id: ApplicationId) =
        runtimeService.correlateByBusinessKey(Messages.MIRAVELO_HANDOVER_REPORTED.value, id.value.toString())

    override fun correlateApplicationWithdrawn(id: ApplicationId) =
        runtimeService.correlateByBusinessKey(Messages.MIRAVELO_APPLICATION_WITHDRAWN.value, id.value.toString())

    override fun completeCreditCheck(id: ApplicationId, solvent: Boolean) =
        taskService.completeTask(
            businessKey = id.value.toString(),
            taskDefinitionKey = FlowNodes.UserTaskCheckCreditRating.ELEMENT_ID,
            variables = mapOf(FlowNodes.UserTaskCheckCreditRating.Variables.SOLVENT.value to solvent),
        )

    override fun completeReversal(id: ApplicationId) =
        taskService.completeTask(
            businessKey = id.value.toString(),
            taskDefinitionKey = FlowNodes.UserTaskReverseApplication.ELEMENT_ID,
        )

    override fun findOpenTasks(): List<BackOfficeTask> {
        val tasks = taskService.findOpenTasks(*taskTypes.keys.toTypedArray())
        val businessKeys = runtimeService.businessKeysById(tasks.map { it.processInstanceId })
        return tasks.mapNotNull { task ->
            val businessKey = businessKeys[task.processInstanceId] ?: return@mapNotNull null
            BackOfficeTask(
                applicationId = ApplicationId.of(businessKey),
                type = taskTypes.getValue(task.taskDefinitionKey),
                waitingSince = task.createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            )
        }
    }
}
