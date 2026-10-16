package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ListOpenTasksQuery
import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.BackOfficeTaskType
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.LocalDateTime

@WebMvcTest(ListOpenTasksController::class)
class ListOpenTasksControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: ListOpenTasksQuery

    @Test
    fun `lists the open tasks with their application`() {
        val application = testLeasingApplication()
        val task = BackOfficeTask(application.id, BackOfficeTaskType.REVERSAL, LocalDateTime.of(2024, 1, 16, 8, 0))
        every { query.openTasks() } returns listOf(ListOpenTasksQuery.Item(task, application))

        val response = mockMvc.perform(get("/api/tasks")).andReturn()

        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(
            "\"type\":\"REVERSAL\"",
            "\"waitingSince\":\"2024-01-16T08:00:00\"",
            "\"applicationId\":\"${application.id.value}\"",
            "\"customerName\":\"John Doe\"",
        )
    }
}
