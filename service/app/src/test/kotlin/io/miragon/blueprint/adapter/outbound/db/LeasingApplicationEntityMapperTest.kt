package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.PolicyId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LeasingApplicationEntityMapperTest {

    @Test
    fun `toEntity copies every field including the optional references`() {
        val application = testLeasingApplication(
            status = LeasingStatus.ACTIVE,
            orderId = OrderId("ORDER-1"),
            contractId = ContractId("CONTRACT-1"),
            policyId = PolicyId("POLICY-1"),
        )

        val entity = LeasingApplicationEntityMapper.toEntity(application)

        assertThat(entity).isEqualTo(
            LeasingApplicationEntity(
                applicationId = application.id.value,
                customerName = "John Doe",
                email = "john.doe@test.com",
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = "BIKE-900",
                bikeModel = "Gravel Explorer 900",
                status = LeasingStatus.ACTIVE,
                orderId = "ORDER-1",
                contractId = "CONTRACT-1",
                policyId = "POLICY-1",
                createdAt = application.createdAt,
            ),
        )
    }

    @Test
    fun `toDomain reverses toEntity with and without the optional fields`() {
        val complete = testLeasingApplication(
            orderId = OrderId("ORDER-1"),
            contractId = ContractId("CONTRACT-1"),
            policyId = PolicyId("POLICY-1"),
        )
        val bare = testLeasingApplication(bikeModel = null)

        assertThat(LeasingApplicationEntityMapper.toDomain(LeasingApplicationEntityMapper.toEntity(complete)))
            .isEqualTo(complete)
        assertThat(LeasingApplicationEntityMapper.toDomain(LeasingApplicationEntityMapper.toEntity(bare)))
            .isEqualTo(bare)
    }
}
