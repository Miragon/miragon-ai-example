package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.CompleteCreditCheckUseCase
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(CompleteCreditCheckController::class)
class CompleteCreditCheckControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: CompleteCreditCheckUseCase

    private val pathVar = "123e4567-e89b-12d3-a456-426614174000"

    private fun creditCheck(body: String) =
        post("/api/bike-leasing/{applicationId}/credit-check", pathVar)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)

    @Test
    fun `back office records a solvent applicant`() {
        every { useCase.completeCreditCheck(any(), any()) } just Runs

        val response = mockMvc.perform(creditCheck("""{"solvent":true}""")).andReturn()

        assertThat(response.response.status).isEqualTo(202)
        verify { useCase.completeCreditCheck(ApplicationId.of(pathVar), true) }
        confirmVerified(useCase)
    }

    @Test
    fun `back office records a non-solvent applicant`() {
        every { useCase.completeCreditCheck(any(), any()) } just Runs

        val response = mockMvc.perform(creditCheck("""{"solvent":false}""")).andReturn()

        assertThat(response.response.status).isEqualTo(202)
        verify { useCase.completeCreditCheck(ApplicationId.of(pathVar), false) }
    }

    @Test
    fun `refuses a decision without the solvent flag`() {
        val response = mockMvc.perform(creditCheck("{}")).andReturn()

        assertThat(response.response.status).isEqualTo(400)
        confirmVerified(useCase)
    }
}
