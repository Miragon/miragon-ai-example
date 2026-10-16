package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class ListLeasingApplicationsController(
    private val query: ListLeasingApplicationsQuery,
) {

    @Operation(
        operationId = "listLeasingApplications",
        responses = [
            ApiResponse(responseCode = "200", description = "A page of leasing applications, newest first"),
            ApiResponse(
                responseCode = "400",
                description = "Invalid paging parameters",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun list(
        @RequestParam(required = false) status: LeasingStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): LeasingApplicationPageDto {
        require(page >= 0) { "page must not be negative" }
        require(size in 1..MAX_PAGE_SIZE) { "size must be between 1 and $MAX_PAGE_SIZE" }
        val result = query.list(ListLeasingApplicationsQuery.Filter(status = status, page = page, size = size))
        return LeasingApplicationPageDto(
            items = result.items.map { LeasingApplicationDto.of(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    data class LeasingApplicationPageDto(
        val items: List<LeasingApplicationDto>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
