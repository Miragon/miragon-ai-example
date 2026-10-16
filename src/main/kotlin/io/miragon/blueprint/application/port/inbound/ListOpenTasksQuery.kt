package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.LeasingApplication

/** The back-office inbox: every open user task together with the application it is about. */
interface ListOpenTasksQuery {
    fun openTasks(): List<Item>

    data class Item(
        val task: BackOfficeTask,
        val application: LeasingApplication,
    )
}
