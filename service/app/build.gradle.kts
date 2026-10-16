import io.miragon.bpmn.adapter.GenerateBpmnModelsTask
import io.miragon.bpmn.domain.shared.OutputLanguage
import io.miragon.bpmn.domain.shared.ProcessEngine
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.math.BigDecimal

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springframework)
    alias(libs.plugins.spring.dependency)
    alias(libs.plugins.bpmnToCode)
    alias(libs.plugins.pitest)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

springBoot {
    buildInfo()
}

configurations.all {
    exclude(group = "org.cibseven.webapp", module = "cibseven-webclient-web")
}

dependencies {
    implementation(libs.bundles.defaultService)
    implementation(libs.bundles.database)
    implementation(libs.bundles.cibseven)
    implementation(libs.springdoc)
    implementation(libs.bpmn.to.code.runtime)
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.cib7ProcessTest)
    testImplementation(libs.bpmn.to.code.testing)
    testImplementation(project(":service:common-architecture-tests"))
}

// The executable process models are documentation first: they live in the designIQ folder next to the
// use case that explains them (docs/use-cases/bike-leasing/process/) and are packaged from there onto
// the classpath, as bpmn/*.bpmn and dmn/*.dmn, so the engine's classpath*:**/*.bpmn deployment and the
// model validation test find them where they always were.
val processModels = rootProject.layout.projectDirectory.dir("docs/use-cases/bike-leasing/process")

tasks.processResources {
    from(processModels) {
        include("*.bpmn")
        into("bpmn")
    }
    from(processModels) {
        include("*.dmn")
        into("dmn")
    }
}

// The plugin declares the matched *.bpmn files as task inputs, so the API is regenerated whenever a
// model changes and the task is UP-TO-DATE otherwise.
val generateBpmnModels = tasks.register<GenerateBpmnModelsTask>("generateBpmnModels") {
    baseDir = rootDir.toString()
    filePattern = "docs/use-cases/bike-leasing/process/*.bpmn"
    outputFolderPath = "$projectDir/src/main/kotlin"
    packagePath = "io.miragon.blueprint.adapter.process"
    outputLanguage = OutputLanguage.KOTLIN
    processEngine = ProcessEngine.CAMUNDA_7
}

// Generate before compiling, so a changed model never compiles against a stale process API.
tasks.compileKotlin {
    dependsOn(generateBpmnModels)
}

tasks.test {
    useJUnitPlatform()
    forkEvery = 1
}

val mutationTargetClasses = (project.findProperty("mutationTargetClasses") as String?)
    ?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)

pitest {
    junit5PluginVersion.set("1.2.2")
    targetClasses.set(mutationTargetClasses ?: listOf("io.miragon.blueprint.*"))
    targetTests.set(listOf("io.miragon.blueprint.*"))
    failWhenNoMutations.set(false)
    excludedClasses.set(
        listOf(
            "io.miragon.blueprint.adapter.process.*",
            "io.miragon.blueprint.CibsevenBikeLeasingApplication*",
            "io.miragon.blueprint.adapter.inbound.cibseven.*",
        ),
    )
    excludedTestClasses.set(
        listOf(
            "io.miragon.blueprint.process.*",
            "io.miragon.blueprint.architecture.*",
        ),
    )
    threads.set(Runtime.getRuntime().availableProcessors())
    timeoutFactor.set(BigDecimal("2.0"))
    avoidCallsTo.set(listOf("kotlin.jvm.internal", "mu", "org.slf4j", "io.github.oshai"))
    mutators.set(listOf("DEFAULTS"))
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    mutationThreshold.set(80)
}

tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// OCI image for the backend, built by Spring's Cloud Native Buildpacks integration — no Dockerfile to
// maintain (layered, non-root by default). See docs/adr/0011 and the "Run it in containers" section of
// CONTRIBUTING.md. Build with `./gradlew bootBuildImage`.
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("miravelo/cibseven-embedded-example:${project.version}")
    // Pin the JVM the buildpack installs to the version the code targets.
    environment.set(mapOf("BP_JVM_VERSION" to "21"))
}
