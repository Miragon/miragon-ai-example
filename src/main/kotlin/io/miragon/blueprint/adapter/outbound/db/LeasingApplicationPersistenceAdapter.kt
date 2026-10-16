package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class LeasingApplicationPersistenceAdapter(
    private val repository: LeasingApplicationJpaRepository,
) : LeasingApplicationRepository {

    override fun save(application: LeasingApplication): LeasingApplication {
        val entity = repository.save(LeasingApplicationEntityMapper.toEntity(application))
        return LeasingApplicationEntityMapper.toDomain(entity)
    }

    override fun findById(id: ApplicationId): LeasingApplication? =
        repository.findByApplicationId(id.value)?.let { LeasingApplicationEntityMapper.toDomain(it) }

    override fun findAllByIds(ids: Collection<ApplicationId>): List<LeasingApplication> =
        repository.findAllById(ids.map { it.value }).map { LeasingApplicationEntityMapper.toDomain(it) }

    override fun findAll(criteria: LeasingApplicationRepository.Criteria): LeasingApplicationRepository.Page {
        val newestFirst = PageRequest.of(criteria.page, criteria.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result =
            criteria.status
                ?.let { repository.findAllByStatus(it, newestFirst) }
                ?: repository.findAll(newestFirst)
        return LeasingApplicationRepository.Page(
            items = result.content.map { LeasingApplicationEntityMapper.toDomain(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
