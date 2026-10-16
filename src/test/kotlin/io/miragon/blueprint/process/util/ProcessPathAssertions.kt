package io.miragon.blueprint.process.util

import io.miragon.bpmn.runtime.path.ProcessPath
import org.cibseven.bpm.engine.test.assertions.bpmn.ProcessInstanceAssert

/**
 * Asserts that the instance passed every node of a compile-checked [ProcessPath] in walk order.
 * Only meaningful within one sequential branch — walk parallel branches as separate paths.
 */
fun ProcessInstanceAssert.hasPassedInOrder(path: ProcessPath<*>): ProcessInstanceAssert =
    hasPassedInOrder(*path.ids)

