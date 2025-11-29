package app.habits.habits

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/** When a habit is meant to be done. */
sealed interface Schedule {
    /** Every day. */
    data object Daily : Schedule

    /** On fixed days of the week, e.g. Monday, Wednesday and Friday. */
    data class Weekdays(
        val days: Set<DayOfWeek>,
    ) : Schedule {
        init {
            require(days.isNotEmpty()) { "A weekday schedule needs at least one day" }
        }
    }

    /** A number of times per week, on any days. */
    data class TimesPerWeek(
        val times: Int,
    ) : Schedule {
        init {
            require(times in 1..7) { "times must be between 1 and 7" }
        }
    }

    /**
     * Whether [date] is a day the habit is expected on. A weekly goal can be met on any day,
     * so every day counts as scheduled for it.
     */
    fun isScheduledOn(date: LocalDate): Boolean =
        when (this) {
            Daily -> true
            is Weekdays -> date.dayOfWeek in days
            is TimesPerWeek -> true
        }

    val isWeekly get() = this is TimesPerWeek
}

@Serializable
data class ScheduleDto(
    val type: String = "",
    val days: List<Int>? = null,
    val timesPerWeek: Int? = null,
)

fun Schedule.toDto(): ScheduleDto =
    when (this) {
        Schedule.Daily -> ScheduleDto("daily")
        is Schedule.Weekdays -> ScheduleDto("weekdays", days = days.map { it.value }.sorted())
        is Schedule.TimesPerWeek -> ScheduleDto("weekly", timesPerWeek = times)
    }

/** Column values for the habits table: type, comma separated ISO day numbers, times per week. */
data class ScheduleColumns(
    val type: String,
    val days: String?,
    val timesPerWeek: Int?,
)

fun Schedule.toColumns(): ScheduleColumns =
    when (this) {
        Schedule.Daily -> ScheduleColumns("DAILY", null, null)
        is Schedule.Weekdays -> ScheduleColumns("WEEKDAYS", days.map { it.value }.sorted().joinToString(","), null)
        is Schedule.TimesPerWeek -> ScheduleColumns("WEEKLY", null, times)
    }

fun scheduleFromColumns(
    type: String,
    days: String?,
    timesPerWeek: Int?,
): Schedule =
    when (type) {
        "DAILY" -> Schedule.Daily
        "WEEKDAYS" ->
            Schedule.Weekdays(
                days
                    .orEmpty()
                    .split(",")
                    .filter { it.isNotBlank() }
                    .map { DayOfWeek.of(it.trim().toInt()) }
                    .toSet(),
            )
        "WEEKLY" -> Schedule.TimesPerWeek(timesPerWeek ?: 1)
        else -> error("Unknown schedule type $type")
    }
