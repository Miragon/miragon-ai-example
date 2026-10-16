package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.domain.leasing.ActionNotAvailableException
import io.miragon.blueprint.domain.leasing.ApplicationNotFoundException
import mu.KotlinLogging
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException
import org.cibseven.bpm.engine.OptimisticLockingException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

/**
 * Cross-cutting web error handling for the REST adapter: turns domain and input errors into RFC 9457
 * `application/problem+json` responses (Spring serialises [ProblemDetail] as such automatically).
 *
 * This is web configuration, hence the `Configuration` suffix whitelisted for `adapter.inbound.rest`
 * and its home here rather than in a new top-level `config` package (which the architecture tests
 * would reject).
 */
@RestControllerAdvice
class GlobalExceptionConfiguration {

    private val log = KotlinLogging.logger {}

    /** Invalid input — a bad UUID, a blank value object, a broken domain invariant (e.g. no income). */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ProblemDetail {
        log.debug { "Bad request: ${exception.message}" }
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.message)
    }

    @ExceptionHandler(ApplicationNotFoundException::class)
    fun handleNotFound(exception: ApplicationNotFoundException): ProblemDetail {
        log.debug { "Not found: ${exception.message}" }
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.message)
    }

    /**
     * Action not available — the application's status forbids it, the process token is no longer at
     * the expected wait state, no such task is open, or a job changed the instance concurrently.
     */
    @ExceptionHandler(
        ActionNotAvailableException::class,
        MismatchingMessageCorrelationException::class,
        OptimisticLockingException::class,
    )
    fun handleActionNotAvailable(exception: RuntimeException): ProblemDetail {
        log.debug { "Action not available: ${exception.message}" }
        return problem(HttpStatus.CONFLICT, "Action not available in the current state", exception.message)
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatus(status).apply {
            this.title = title
            this.detail = detail
            this.type = URI.create("https://miravelo.example/problems/${status.value()}")
        }
}
