package io.miragon.blueprint.process.util

import io.miragon.bpmn.runtime.FlowNode
import org.cibseven.bpm.engine.ProcessEngine

/**
 * Executes the async-continuation job of a **specific** element (by activity id), rather than
 * whichever job happens to be next. This lets a test read as an explicit, ordered trace of the
 * process and fails loudly (no job found) if the expected continuation is missing — which is the
 * safer, more readable choice for the deterministic, linear parts of a flow.
 *
 * The `camunda:asyncAfter` flags in the model create exactly these jobs after each step, and the job
 * executor is disabled in tests, so the test drives them by hand.
 */
fun ProcessEngine.executeJobFor(activity: FlowNode) {
    val job =
        managementService
            .createJobQuery()
            .messages()
            .activityId(activity.id.value)
            .singleResult()
    requireNotNull(job) { "no async-continuation job found for activity '${activity.id.value}'" }
    managementService.executeJob(job.id)
}

/**
 * Drives *all* pending async-continuation jobs until the process reaches its next wait state. Use
 * this where the exact sequence of continuations is engine-ordered and not worth enumerating — e.g.
 * the compensation chain, whose handlers run in an implementation-defined order. For linear,
 * deterministic steps prefer [executeJobFor], which is explicit about which continuation it fires.
 */
fun ProcessEngine.continueToNextWaitState(maxIterations: Int = 50) {
    repeat(maxIterations) {
        val job =
            managementService
                .createJobQuery()
                .active()
                .messages()
                .listPage(0, 1)
                .firstOrNull() ?: return
        managementService.executeJob(job.id)
    }
}

/**
 * Executes the job of [activity] again and again, the way the job executor would after each retry
 * interval, until its retries are used up and the engine raises an incident. Every attempt is expected
 * to fail — a successful attempt fails the test. Returns the failures, one per attempt, so a test can
 * assert both how often the engine retried and why the job failed.
 */
fun ProcessEngine.failJobUntilRetriesAreExhausted(activity: FlowNode, maxAttempts: Int = 10): List<Throwable> {
    val failures = mutableListOf<Throwable>()
    repeat(maxAttempts) {
        val job =
            managementService
                .createJobQuery()
                .activityId(activity.id.value)
                .withRetriesLeft()
                .singleResult() ?: return failures
        val failure = runCatching { managementService.executeJob(job.id) }.exceptionOrNull()
        requireNotNull(failure) { "the job of activity '${activity.id.value}' was expected to fail but succeeded" }
        failures += failure
    }
    error("the retries of activity '${activity.id.value}' were not exhausted after $maxAttempts attempts")
}
