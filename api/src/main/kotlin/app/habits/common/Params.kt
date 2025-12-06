package app.habits.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun ApplicationCall.longPath(name: String): Long =
    parameters[name]?.toLongOrNull() ?: throw NotFoundException("Resource")

fun ApplicationCall.datePath(name: String): LocalDate =
    parseDate(name, parameters[name])
        ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_date", "'$name' must be a date like 2026-01-31")

fun ApplicationCall.dateQuery(name: String): LocalDate? = parseDate(name, request.queryParameters[name])

private fun parseDate(
    name: String,
    raw: String?,
): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_date", "'$name' must be a date like 2026-01-31")
    }
}
