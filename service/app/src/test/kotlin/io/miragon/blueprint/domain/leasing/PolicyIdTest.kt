package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PolicyIdTest {

    @Test
    fun `keeps a non-blank value`() {
        assertThat(PolicyId("POLICY-1").value).isEqualTo("POLICY-1")
    }

    @Test
    fun `refuses a blank value`() {
        assertThatThrownBy { PolicyId(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("PolicyId must not be blank")
    }
}
