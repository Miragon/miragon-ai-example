package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(ListLeasingApplicationsController::class)
class ListLeasingApplicationsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: ListLeasingApplicationsQuery

    @Test
    fun `lists a page of applications filtered by status`() {
        val application = testLeasingApplication(status = LeasingStatus.ORDERED)
        val filter = ListLeasingApplicationsQuery.Filter(LeasingStatus.ORDERED, page = 1, size = 5)
        every { query.list(filter) } returns
            ListLeasingApplicationsQuery.Page(listOf(application), page = 1, size = 5, totalElements = 6, totalPages = 2)

        val response = mockMvc.perform(get("/api/bike-leasing?status=ORDERED&page=1&size=5")).andReturn()

        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(
            "\"applicationId\":\"${application.id.value}\"",
            "\"status\":\"ORDERED\"",
            "\"page\":1",
            "\"size\":5",
            "\"totalElements\":6",
            "\"totalPages\":2",
        )
        verify { query.list(filter) }
        confirmVerified(query)
    }

    @Test
    fun `lists the first page of all applications by default`() {
        val filter = ListLeasingApplicationsQuery.Filter(status = null, page = 0, size = 20)
        every { query.list(filter) } returns
            ListLeasingApplicationsQuery.Page(emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0)

        val response = mockMvc.perform(get("/api/bike-leasing")).andReturn()

        assertThat(response.response.status).isEqualTo(200)
        verify { query.list(filter) }
    }

    @Test
    fun `refuses an unknown status`() {
        val response = mockMvc.perform(get("/api/bike-leasing?status=LOST")).andReturn()

        assertThat(response.response.status).isEqualTo(400)
        confirmVerified(query)
    }

    @Test
    fun `refuses a negative page and a size outside 1 to 100`() {
        every { query.list(any()) } returns
            ListLeasingApplicationsQuery.Page(emptyList(), page = 0, size = 100, totalElements = 0, totalPages = 0)
        assertThat(mockMvc.perform(get("/api/bike-leasing?page=-1")).andReturn().response.status).isEqualTo(400)
        assertThat(mockMvc.perform(get("/api/bike-leasing?size=0")).andReturn().response.status).isEqualTo(400)
        assertThat(mockMvc.perform(get("/api/bike-leasing?size=101")).andReturn().response.status).isEqualTo(400)
        assertThat(mockMvc.perform(get("/api/bike-leasing?size=100")).andReturn().response.status).isEqualTo(200)
    }
}
