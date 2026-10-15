package io.miragon.blueprint.domain.leasing

/**
 * Lifecycle of a leasing application, mirrored from the process:
 * [RECEIVED] on submission, [ORDERED] once a bike order exists, [HANDED_OVER] once the bike is
 * handed to the customer (the withdrawal period is running), [ACTIVE] when the leasing is live after
 * the withdrawal period elapses. [REJECTED] (not solvent, or the contract was not signed within the
 * deadline) and [CANCELLED] (no acceptable alternative bike, so the contract was reversed) are the
 * terminal negative outcomes.
 */
enum class LeasingStatus {
    RECEIVED,
    ORDERED,
    HANDED_OVER,
    ACTIVE,
    REJECTED,
    CANCELLED,
}
