package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.application.port.inbound.ListOpenTasksQuery
import io.miragon.blueprint.domain.leasing.BackOfficeTaskType
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * The back-office inbox. It carries no engine task id on purpose: a task is completed through the
 * domain (`POST /api/bike-leasing/{id}/credit-check` or `/reversal`), correlated by application id.
 */
@RestController
@RequestMapping("/api/tasks")
class ListOpenTasksController(
    private val query: ListOpenTasksQuery,
) {

    @Operation(operationId = "listOpenTasks")
    @GetMapping
    fun openTasks(): List<OpenTaskDto> =
        query.openTasks().map {
            OpenTaskDto(
                type = it.task.type,
                waitingSince = it.task.waitingSince,
                application = LeasingApplicationDto.of(it.application),
            )
        }

    data class OpenTaskDto(
        val type: BackOfficeTaskType,
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val waitingSince: LocalDateTime,
        val application: LeasingApplicationDto,
    )
}
