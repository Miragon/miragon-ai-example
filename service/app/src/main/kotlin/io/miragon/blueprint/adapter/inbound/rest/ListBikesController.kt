package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bikes")
class ListBikesController(
    private val query: ListBikesQuery,
) {

    @Operation(operationId = "listBikes")
    @GetMapping
    fun all(): List<BikeDto> = query.all().map { BikeDto(bikeId = it.bikeId.value, model = it.model) }

    data class BikeDto(
        val bikeId: String,
        val model: String,
    )
}
