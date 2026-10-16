package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus

/**
 * Reads a page of leasing applications, newest first, optionally filtered by status. The port owns
 * its [Filter] and [Page] types; Spring Data paging never crosses into the application layer.
 */
interface ListLeasingApplicationsQuery {
    fun list(filter: Filter): Page

    data class Filter(
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
