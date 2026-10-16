package io.miragon.blueprint.domain.leasing

/**
 * Raised when an application breaks a business rule checked by "Validate application". The inbound
 * CIB seven adapter translates it into the BPMN error `applicationInvalid`, which routes the process
 * to the rejection.
 */
class ApplicationInvalidException(
    id: ApplicationId,
    violations: List<String>,
) : RuntimeException("Application ${id.value} is invalid: ${violations.joinToString("; ")}")
