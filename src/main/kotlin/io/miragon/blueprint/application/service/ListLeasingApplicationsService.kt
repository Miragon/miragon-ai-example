package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ListLeasingApplicationsService(
    private val repository: LeasingApplicationRepository,
) : ListLeasingApplicationsQuery {

    override fun list(filter: ListLeasingApplicationsQuery.Filter): ListLeasingApplicationsQuery.Page {
        val page = repository.findAll(
            LeasingApplicationRepository.Criteria(status = filter.status, page = filter.page, size = filter.size),
        )
        return ListLeasingApplicationsQuery.Page(
            items = page.items,
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
