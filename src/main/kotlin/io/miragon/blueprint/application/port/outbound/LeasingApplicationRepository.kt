package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus

/** Persistence port for the leasing application aggregate. */
interface LeasingApplicationRepository {
    fun save(application: LeasingApplication): LeasingApplication

    fun findById(id: ApplicationId): LeasingApplication?

    fun findAllByIds(ids: Collection<ApplicationId>): List<LeasingApplication>

    fun findAll(criteria: Criteria): Page

    data class Criteria(
        val status: LeasingStatus?,
        val page: Int,
        val size: Int,
    )

    data class Page(
        val items: List<LeasingApplication>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
}
