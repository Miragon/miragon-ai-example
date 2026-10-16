package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class GetLeasingApplicationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = GetLeasingApplicationService(repository = repository)

    @Test
    fun `byId returns the stored application`() {

        // given: a stored application
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application

        // when / then: the query returns it unchanged
        assertThat(underTest.byId(application.id)).isEqualTo(application)
        verify { repository.findById(application.id) }
        confirmVerified(repository)
    }

    @Test
    fun `byId returns null when the application does not exist`() {

        // given: an unknown application id
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        every { repository.findById(id) } returns null

        // when / then: the query returns null
        assertThat(underTest.byId(id)).isNull()
        verify { repository.findById(id) }
        confirmVerified(repository)
    }
}
