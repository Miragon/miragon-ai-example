package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase
import io.miragon.blueprint.domain.leasing.ActionNotAvailableException
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException
import org.cibseven.bpm.engine.OptimisticLockingException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(ReportHandoverController::class)
class GlobalExceptionConfigurationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ReportHandoverUseCase

    private val pathVar = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun `MismatchingMessageCorrelationException maps to 409 with problem+json`() {

        // given: a correlation that fails because the token is no longer at the expected wait state
        every { useCase.reportHandover(any()) } throws
            MismatchingMessageCorrelationException("miravelo.handoverReported", "No matching token")

        // when: the POST is performed
        val response = reportHandover(pathVar)

        // then: 409 Conflict is returned as an RFC 9457 problem with its title and type
        assertProblem(response, status = 409, title = "Action not available in the current state")
    }

    @Test
    fun `ActionNotAvailableException maps to 409`() {
        every { useCase.reportHandover(any()) } throws ActionNotAvailableException("No open task")

        assertProblem(reportHandover(pathVar), status = 409, title = "Action not available in the current state")
    }

    @Test
    fun `OptimisticLockingException maps to 409`() {
        every { useCase.reportHandover(any()) } throws OptimisticLockingException("concurrent update")

        assertProblem(reportHandover(pathVar), status = 409, title = "Action not available in the current state")
    }

    @Test
    fun `ApplicationNotFoundException maps to 404 with the message as detail`() {

        // given: the service cannot find the application
        every { useCase.reportHandover(any()) } throws ApplicationNotFoundException(ApplicationId.of(pathVar))

        // when: the POST is performed
        val response = reportHandover(pathVar)

        // then: 404 Not Found is returned as a problem that names the missing application
        assertProblem(response, status = 404, title = "Resource not found")
        assertThat(response.contentAsString).contains("\"detail\":\"Unknown application $pathVar\"")
    }

    @Test
    fun `IllegalArgumentException maps to 400 with the message as detail`() {

        // when: the POST is performed with an application id that is no UUID
        val response = reportHandover("not-a-uuid")

        // then: 400 Bad Request is returned as a problem, before the use case is reached
        assertProblem(response, status = 400, title = "Invalid request")
        assertThat(response.contentAsString).contains("not-a-uuid")
    }

    private fun reportHandover(applicationId: String): MockHttpServletResponse =
        mockMvc
            .perform(post("/api/bike-leasing/{applicationId}/report-handover", applicationId))
            .andReturn()
            .response

    private fun assertProblem(response: MockHttpServletResponse, status: Int, title: String) {
        assertThat(response.status).isEqualTo(status)
        assertThat(response.contentType).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(response.contentAsString).contains(
            "\"title\":\"$title\"",
            "\"type\":\"https://miravelo.example/problems/$status\"",
        )
    }
}
