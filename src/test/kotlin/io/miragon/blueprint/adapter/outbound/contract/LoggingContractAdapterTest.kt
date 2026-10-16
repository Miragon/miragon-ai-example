package io.miragon.blueprint.adapter.outbound.contract

import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LoggingContractAdapterTest {

    private val underTest = LoggingContractAdapter()

    @Test
    fun `issueContract mints a fresh contract id`() {
        val first = underTest.issueContract(testLeasingApplication().id)
        val second = underTest.issueContract(testLeasingApplication().id)
        assertThat(first.value).startsWith("CONTRACT-")
        assertThat(first).isNotEqualTo(second)
    }
}
