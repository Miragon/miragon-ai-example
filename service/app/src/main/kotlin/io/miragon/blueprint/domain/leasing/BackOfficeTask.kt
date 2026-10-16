package io.miragon.blueprint.domain.leasing

import java.time.LocalDateTime

/**
 * A user task of the process waiting for the back office, seen from the domain: which application,
 * which kind of work and since when. It carries no engine task id; the task is completed through the
 * domain, correlated by the application id.
 */
data class BackOfficeTask(
    val applicationId: ApplicationId,
    val type: BackOfficeTaskType,
    val waitingSince: LocalDateTime,
)
