package app.habits.common

import io.ktor.http.HttpStatusCode

/** An error that maps directly to an HTTP response with the shared error body. */
open class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val fields: Map<String, String> = emptyMap(),
) : RuntimeException(message)

class ValidationException(
    fields: Map<String, String>,
) : ApiException(HttpStatusCode.BadRequest, "validation_failed", "Some fields are invalid", fields)

class NotFoundException(
    what: String,
) : ApiException(HttpStatusCode.NotFound, "not_found", "$what not found")

class ConflictException(
    code: String,
    message: String,
) : ApiException(HttpStatusCode.Conflict, code, message)
