package io.miragon.blueprint.domain.leasing

class ApplicationNotFoundException(
    id: ApplicationId,
) : RuntimeException("Unknown application ${id.value}")
