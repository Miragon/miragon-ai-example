package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(LeasingApplicationPersistenceAdapter::class)
class LeasingApplicationPersistenceAdapterTest {

    @Autowired
    private lateinit var underTest: LeasingApplicationPersistenceAdapter

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `saves and reloads an application`() {

        // given: a leasing application
        val application = testLeasingApplication(id = id)

        // when: it is saved and re-read from a cleared persistence context
        underTest.save(application)
        entityManager.flush()
        entityManager.clear()

        // then: the reloaded application equals the original
        assertThat(underTest.findById(id)).usingRecursiveComparison().isEqualTo(application)
    }

    @Test
    fun `findById returns null when the application does not exist`() {

        // given: an empty database
        // when / then: the lookup returns null
        assertThat(underTest.findById(id)).isNull()
    }

    @Test
    @Sql("classpath:sql/leasing-application.sql")
    fun `findById maps an existing row back to the domain`() {

        // given: a pre-inserted row (see sql/leasing-application.sql)
        // when / then: the adapter maps it back to the expected application
        assertThat(underTest.findById(id))
            .usingRecursiveComparison()
            .isEqualTo(testLeasingApplication(id = id))
    }

    @Test
    fun `save overwrites a stored application with its new state and returns it`() {

        // given: a stored application that is later ordered
        val application = testLeasingApplication(id = id)
        underTest.save(application)
        entityManager.flush()
        entityManager.clear()
        val ordered = application.documentOrder(OrderId("ORDER-1"))

        // when: the new state is saved
        val saved = underTest.save(ordered)
        entityManager.flush()
        entityManager.clear()

        // then: the returned and the reloaded application both carry the order
        assertThat(saved).usingRecursiveComparison().isEqualTo(ordered)
        assertThat(underTest.findById(id)?.status).isEqualTo(LeasingStatus.ORDERED)
        assertThat(underTest.findById(id)?.orderId).isEqualTo(OrderId("ORDER-1"))
    }
}
