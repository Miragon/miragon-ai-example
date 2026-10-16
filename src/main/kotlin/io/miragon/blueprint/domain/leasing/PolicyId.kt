package io.miragon.blueprint.domain.leasing

/** Reference to the insurance policy the (external) insurer issued for the leased bike. */
@JvmInline
value class PolicyId(val value: String) {
    init {
        require(value.isNotBlank()) { "PolicyId must not be blank" }
    }
}
