package io.miragon.blueprint.architecture

import io.miragon.common.architecture.ServiceArchitectureTest

/**
 * Runs the reusable ArchUnit + Konsist suite against this service's base package.
 * The rules live in the `io.miragon.common.architecture` test package.
 */
class ArchitectureTest : ServiceArchitectureTest("io.miragon.blueprint")
