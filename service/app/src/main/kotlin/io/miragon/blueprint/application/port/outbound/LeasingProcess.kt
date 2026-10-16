package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.BackOfficeTask
import io.miragon.blueprint.domain.leasing.LeasingApplication

/**
 * Outbound port that drives the BPMN engine: it starts a process instance, correlates the messages
 * that release the process's wait states (contract signed, handover reported, application withdrawn),
 * completes the two back-office user tasks and lists the open ones. Implemented by the CIB seven
 * adapter.
 */
interface LeasingProcess {
    fun submitRequest(application: LeasingApplication)

    fun correlateContractSigned(id: ApplicationId)

    fun correlateHandoverReported(id: ApplicationId)

    fun correlateApplicationWithdrawn(id: ApplicationId)

    fun completeCreditCheck(id: ApplicationId, solvent: Boolean)

    fun completeReversal(id: ApplicationId)

    fun findOpenTasks(): List<BackOfficeTask>
}
