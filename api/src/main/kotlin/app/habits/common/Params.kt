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

/** Inclusive date range, validated so that callers can't ask for an unbounded amount of work. */
data class DateRange(
    val from: LocalDate,
    val to: LocalDate,
) {
    init {
        require(!to.isBefore(from)) { "Range end is before its start" }
    }

    fun days(): Sequence<LocalDate> = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }

    operator fun contains(date: LocalDate) = !date.isBefore(from) && !date.isAfter(to)
}

fun ApplicationCall.dateRange(
    defaultFrom: LocalDate,
    defaultTo: LocalDate,
): DateRange {
    val from = dateQuery("from") ?: defaultFrom
    val to = dateQuery("to") ?: defaultTo
    if (to.isBefore(from)) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_range", "'to' must not be before 'from'")
    }
    return DateRange(from, to)
}
