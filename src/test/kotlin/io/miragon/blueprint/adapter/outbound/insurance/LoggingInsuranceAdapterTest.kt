package io.miragon.blueprint.adapter.outbound.insurance

import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LoggingInsuranceAdapterTest {

    private val underTest = LoggingInsuranceAdapter()

    @Test
    fun `issuePolicy mints a fresh policy id`() {
        val first = underTest.issuePolicy(testLeasingApplication().id)
        val second = underTest.issuePolicy(testLeasingApplication().id)
        assertThat(first.value).startsWith("POLICY-")
        assertThat(first).isNotEqualTo(second)
    }
}
