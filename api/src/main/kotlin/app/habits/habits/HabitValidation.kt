package app.habits.habits

import app.habits.common.Validator
import app.habits.common.validate
import java.time.DayOfWeek
import java.time.LocalDate

const val DEFAULT_COLOR = "#4f7cff"
const val MAX_TARGET = 100_000
private val HEX_COLOR = Regex("^#[0-9a-fA-F]{6}$")

/**
 * Checks a create/update request and turns it into a [HabitInput]. [today] is the user's local date and
 * bounds how far ahead a start date may be.
 */
fun HabitRequest.validated(today: LocalDate): HabitInput {
    val defaultStartDate = today
    val name = name.trim()
    val description = description?.trim()
    val unit = unit?.trim()?.ifEmpty { null }
    val color = color?.trim()?.ifEmpty { null } ?: DEFAULT_COLOR
    var schedule: Schedule? = null
    validate {
        check("name", name.isNotEmpty()) { "Give the habit a name" }
        check("name", name.length <= 80) { "Name must be at most 80 characters" }
        check("description", (description?.length ?: 0) <= 500) { "Description must be at most 500 characters" }
        check("color", HEX_COLOR.matches(color)) { "Color must be a hex value like #4f7cff" }
        schedule = parseSchedule(this@validated.schedule)
        check("target", target == null || target in 1..MAX_TARGET) { "Target must be between 1 and $MAX_TARGET" }
        check("unit", (unit?.length ?: 0) <= 20) { "Unit must be at most 20 characters" }
        val start = startDate ?: defaultStartDate
        check("startDate", !start.isBefore(LocalDate.of(2000, 1, 1))) { "Start date is too far in the past" }
    }
    return HabitInput(
        name = name,
        description = description,
        color = color.lowercase(),
        schedule = schedule!!,
        target = target,
        unit = unit,
        startDate = startDate ?: defaultStartDate,
    )
}

private fun Validator.parseSchedule(dto: ScheduleDto?): Schedule? {
    if (dto == null) {
        error("schedule", "Choose a schedule")
        return null
    }
    return when (dto.type.lowercase()) {
        "daily" -> Schedule.Daily
        "weekdays" -> {
            val days = dto.days.orEmpty()
            when {
                days.isEmpty() -> error("schedule.days", "Pick at least one day").let { null }
                days.any { it !in 1..7 } ->
                    error(
                        "schedule.days",
                        "Days are numbered 1 (Monday) to 7 (Sunday)",
                    ).let { null }
                else -> Schedule.Weekdays(days.map { DayOfWeek.of(it) }.toSet())
            }
        }
        "weekly" -> {
            val times = dto.timesPerWeek
            if (times == null || times !in 1..7) {
                error("schedule.timesPerWeek", "Times per week must be between 1 and 7")
                null
            } else {
                Schedule.TimesPerWeek(times)
            }
        }
        else -> error("schedule.type", "Schedule must be daily, weekdays or weekly").let { null }
    }
}
