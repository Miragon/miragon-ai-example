package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LeasingApplicationTest {

    @Test
    fun `receive creates a RECEIVED application with the submitted data`() {
        // given / when: a request with income is received
        val application =
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = CustomerName("Jane Doe"),
                email = Email("jane@test.com"),
                age = 40,
                monthlyNetIncome = 4200.0,
                bikeId = BikeId("BIKE-800"),
                bikeModel = "Carbon Road 800",
                createdAt = LocalDateTime.of(2024, 1, 15, 10, 30),
            )
        // then: the application is RECEIVED and carries the data unchanged
        assertThat(application.status).isEqualTo(LeasingStatus.RECEIVED)
        assertThat(application.age).isEqualTo(40)
        assertThat(application.monthlyNetIncome).isEqualTo(4200.0)
        assertThat(application.bikeModel).isEqualTo("Carbon Road 800")
        assertThat(application.contractId).isNull()
        assertThat(application.orderId).isNull()
        assertThat(application.policyId).isNull()
    }

    @Test
    fun `receive refuses a request without income`() {
        assertThatThrownBy {
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = CustomerName("Jane Doe"),
                email = Email("jane@test.com"),
                age = 40,
                monthlyNetIncome = 0.0,
                bikeId = BikeId("BIKE-800"),
                bikeModel = "Carbon Road 800",
                createdAt = LocalDateTime.of(2024, 1, 15, 10, 30),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Monthly net income must be greater than zero")
    }

    @Test
    fun `validate accepts an adult applicant asking for a catalogue bike`() {
        assertThatCode { testLeasingApplication(age = 18).validate() }.doesNotThrowAnyException()
    }

    @Test
    fun `validate rejects an applicant under 18`() {
        assertThatThrownBy { testLeasingApplication(age = 17).validate() }
            .isInstanceOf(ApplicationInvalidException::class.java)
            .hasMessageContaining("the applicant must be at least 18 years old")
            .hasMessageNotContaining("catalogue")
    }

    @Test
    fun `validate rejects a bike that is not in the catalogue`() {
        assertThatThrownBy { testLeasingApplication(bikeId = BikeId("BIKE-X"), bikeModel = null).validate() }
            .isInstanceOf(ApplicationInvalidException::class.java)
            .hasMessageContaining("bike BIKE-X is not in the catalogue")
            .hasMessageNotContaining("years old")
    }

    @Test
    fun `validate names every violated rule`() {
        val application = testLeasingApplication(age = 16, bikeModel = null)
        assertThatThrownBy { application.validate() }
            .hasMessage(
                "Application ${application.id.value} is invalid: the applicant must be at least 18 years old; " +
                    "bike BIKE-900 is not in the catalogue",
            )
    }

    @Test
    fun `sendContract records the contract and moves to CONTRACT_SENT`() {
        val application = testLeasingApplication()
        val sent = application.sendContract(ContractId("CONTRACT-1"))
        assertThat(sent).isEqualTo(
            application.copy(contractId = ContractId("CONTRACT-1"), status = LeasingStatus.CONTRACT_SENT),
        )
    }

    @Test
    fun `documentOrder records the order and moves to ORDERED`() {
        val application = testLeasingApplication(status = LeasingStatus.CONTRACT_SENT)
        val ordered = application.documentOrder(OrderId("ORDER-1"))
        assertThat(ordered).isEqualTo(application.copy(orderId = OrderId("ORDER-1"), status = LeasingStatus.ORDERED))
    }

    @Test
    fun `documentPolicy records the policy and keeps the status`() {
        val application = testLeasingApplication(status = LeasingStatus.ORDERED)
        val insured = application.documentPolicy(PolicyId("POLICY-1"))
        assertThat(insured).isEqualTo(application.copy(policyId = PolicyId("POLICY-1")))
    }

    @Test
    fun `the terminal transitions set their status`() {
        val application = testLeasingApplication()
        assertThat(application.activate().status).isEqualTo(LeasingStatus.ACTIVE)
        assertThat(application.reject().status).isEqualTo(LeasingStatus.REJECTED)
        assertThat(application.cancel().status).isEqualTo(LeasingStatus.CANCELLED)
    }

    @Test
    fun `signContract moves a sent contract to CONTRACT_SIGNED`() {
        val sent = testLeasingApplication(status = LeasingStatus.CONTRACT_SENT)
        assertThat(sent.signContract()).isEqualTo(sent.copy(status = LeasingStatus.CONTRACT_SIGNED))
    }

    @Test
    fun `signContract is only available while a contract is waiting for the signature`() {
        LeasingStatus.entries.filter { it != LeasingStatus.CONTRACT_SENT }.forEach { status ->
            assertThatThrownBy { testLeasingApplication(status = status).signContract() }
                .isInstanceOf(ActionNotAvailableException::class.java)
                .hasMessage("No contract is waiting for a signature")
        }
    }

    @Test
    fun `withdraw is available while the application is running`() {
        val running = listOf(
            LeasingStatus.RECEIVED,
            LeasingStatus.CONTRACT_SENT,
            LeasingStatus.CONTRACT_SIGNED,
            LeasingStatus.ORDERED,
        )
        running.forEach { status ->
            assertThat(testLeasingApplication(status = status).withdraw().status).isEqualTo(LeasingStatus.WITHDRAWN)
        }
    }

    @Test
    fun `withdraw is refused once the application is finished or already withdrawn`() {
        val closed = listOf(LeasingStatus.ACTIVE, LeasingStatus.REJECTED, LeasingStatus.WITHDRAWN, LeasingStatus.CANCELLED)
        closed.forEach { status ->
            assertThatThrownBy { testLeasingApplication(status = status).withdraw() }
                .isInstanceOf(ActionNotAvailableException::class.java)
                .hasMessage("An application in status $status cannot be withdrawn")
        }
    }
}
