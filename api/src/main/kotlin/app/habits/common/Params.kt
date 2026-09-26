package app.habits.common

import io.ktor.server.application.ApplicationCall
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun ApplicationCall.longPath(name: String): Long =
    parameters[name]?.toLongOrNull() ?: throw NotFoundException("Resource")

fun ApplicationCall.datePath(name: String): LocalDate =
    parseDate(name, parameters[name]) ?: badRequest("invalid_date", "'$name' must be a date like 2026-01-31")

fun ApplicationCall.dateQuery(name: String): LocalDate? = parseDate(name, request.queryParameters[name])

fun ApplicationCall.intQuery(
    name: String,
    default: Int,
    range: IntRange,
): Int {
    val raw = request.queryParameters[name] ?: return default
    val value = raw.toIntOrNull()
    if (value == null || value !in range) {
        badRequest("invalid_parameter", "'$name' must be a number between ${range.first} and ${range.last}")
    }
    return value
}

private fun parseDate(
    name: String,
    raw: String?,
): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        badRequest("invalid_date", "'$name' must be a date like 2026-01-31")
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
    maxDays: Long = 731,
): DateRange {
    val from = dateQuery("from") ?: defaultFrom
    val to = dateQuery("to") ?: defaultTo
    if (to.isBefore(from)) badRequest("invalid_range", "'to' must not be before 'from'")
    if (from.plusDays(maxDays).isBefore(to)) badRequest("invalid_range", "A range can cover at most $maxDays days")
    return DateRange(from, to)
}
