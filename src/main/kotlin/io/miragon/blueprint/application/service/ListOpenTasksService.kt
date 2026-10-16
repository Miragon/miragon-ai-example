package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListOpenTasksQuery
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ListOpenTasksService(
    private val process: LeasingProcess,
    private val repository: LeasingApplicationRepository,
) : ListOpenTasksQuery {

    override fun openTasks(): List<ListOpenTasksQuery.Item> {
        val tasks = process.findOpenTasks()
        val applications = repository.findAllByIds(tasks.map { it.applicationId }).associateBy { it.id }
        return tasks.mapNotNull { task ->
            applications[task.applicationId]?.let { ListOpenTasksQuery.Item(task, it) }
        }
    }
}
