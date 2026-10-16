package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListOpenTasksQuery
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.BackOfficeTaskType
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ListOpenTasksServiceTest {

    private val process = mockk<LeasingProcess>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ListOpenTasksService(process = process, repository = repository)

    @Test
    fun `openTasks pairs every open task with its application in task order`() {
        val first = testLeasingApplication(id = ApplicationId.new())
        val second = testLeasingApplication(id = ApplicationId.new())
        val creditCheck = BackOfficeTask(first.id, BackOfficeTaskType.CREDIT_CHECK, LocalDateTime.of(2024, 1, 1, 9, 0))
        val reversal = BackOfficeTask(second.id, BackOfficeTaskType.REVERSAL, LocalDateTime.of(2024, 1, 2, 9, 0))
        every { process.findOpenTasks() } returns listOf(creditCheck, reversal)
        every { repository.findAllByIds(listOf(first.id, second.id)) } returns listOf(second, first)

        val items = underTest.openTasks()

        assertThat(items).containsExactly(
            ListOpenTasksQuery.Item(creditCheck, first),
            ListOpenTasksQuery.Item(reversal, second),
        )
    }

    @Test
    fun `openTasks skips a task whose application is unknown`() {
        val task = BackOfficeTask(ApplicationId.new(), BackOfficeTaskType.CREDIT_CHECK, LocalDateTime.of(2024, 1, 1, 9, 0))
        every { process.findOpenTasks() } returns listOf(task)
        every { repository.findAllByIds(any()) } returns emptyList()

        assertThat(underTest.openTasks()).isEmpty()
    }
}
