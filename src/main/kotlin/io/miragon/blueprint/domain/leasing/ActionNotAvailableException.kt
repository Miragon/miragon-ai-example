package io.miragon.blueprint.domain.leasing

/** The application or its process is not in a state that allows the requested action. */
class ActionNotAvailableException(
    message: String,
) : RuntimeException(message)
