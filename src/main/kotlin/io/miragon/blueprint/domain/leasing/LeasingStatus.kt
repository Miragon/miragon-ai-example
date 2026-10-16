package io.miragon.blueprint.domain.leasing

/**
 * Lifecycle of a leasing application, mirrored from the process: [RECEIVED] on submission (validation
 * and the manual credit check run), [CONTRACT_SENT] while the customer's signature is awaited,
 * [ORDERED] once the bike is ordered from the dealer, [ACTIVE] after the handover. [REJECTED] (invalid
 * or not solvent) and [CANCELLED] (withdrawn and reversed) are the terminal negative outcomes;
 * [WITHDRAWN] means the customer withdrew and the back office still has to reverse the application.
 */
enum class LeasingStatus {
    RECEIVED,
    CONTRACT_SENT,
    ORDERED,
    ACTIVE,
    REJECTED,
    WITHDRAWN,
    CANCELLED,
}
