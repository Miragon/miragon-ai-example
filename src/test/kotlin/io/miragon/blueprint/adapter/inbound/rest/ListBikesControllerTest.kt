package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(ListBikesController::class)
class ListBikesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: ListBikesQuery

    @Test
    fun `lists the bike catalogue`() {
        every { query.all() } returns listOf(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))

        val response = mockMvc.perform(get("/api/bikes")).andReturn()

        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString)
            .isEqualTo("""[{"bikeId":"BIKE-900","model":"Gravel Explorer 900"}]""")
    }
}
