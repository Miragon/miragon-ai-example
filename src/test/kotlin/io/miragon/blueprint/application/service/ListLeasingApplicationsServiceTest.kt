package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ListLeasingApplicationsServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ListLeasingApplicationsService(repository = repository)

    @Test
    fun `list passes the filter to the repository and returns its page`() {
        val application = testLeasingApplication(status = LeasingStatus.ORDERED)
        every {
            repository.findAll(LeasingApplicationRepository.Criteria(LeasingStatus.ORDERED, page = 1, size = 5))
        } returns LeasingApplicationRepository.Page(listOf(application), page = 1, size = 5, totalElements = 6, totalPages = 2)

        val page = underTest.list(ListLeasingApplicationsQuery.Filter(LeasingStatus.ORDERED, page = 1, size = 5))

        assertThat(page).isEqualTo(
            ListLeasingApplicationsQuery.Page(listOf(application), page = 1, size = 5, totalElements = 6, totalPages = 2),
        )
    }
}
