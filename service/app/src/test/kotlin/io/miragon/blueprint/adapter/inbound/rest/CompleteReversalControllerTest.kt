package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.CompleteReversalUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(CompleteReversalController::class)
class CompleteReversalControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: CompleteReversalUseCase

    @Test
    fun `back office confirms the reversal`() {
        val pathVar = "123e4567-e89b-12d3-a456-426614174000"
        every { useCase.completeReversal(any()) } just Runs

        val response = mockMvc.perform(post("/api/bike-leasing/{applicationId}/reversal", pathVar)).andReturn()

        assertThat(response.response.status).isEqualTo(202)
        verify { useCase.completeReversal(ApplicationId.of(pathVar)) }
        confirmVerified(useCase)
    }
}
