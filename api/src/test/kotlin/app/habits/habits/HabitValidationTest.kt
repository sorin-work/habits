package app.habits.habits

import app.habits.common.ValidationException
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HabitValidationTest {
    private val today = LocalDate.of(2026, 3, 18)

    private fun errors(request: HabitRequest) = assertFailsWith<ValidationException> { request.validated(today) }.fields

    @Test
    fun `fills in defaults for a minimal habit`() {
        val input = HabitRequest(name = "  Stretch ", schedule = ScheduleDto("daily")).validated(today)
        assertEquals("Stretch", input.name)
        assertEquals(DEFAULT_COLOR, input.color)
        assertEquals(today, input.startDate)
        assertEquals(Schedule.Daily, input.schedule)
    }

    @Test
    fun `parses weekday and weekly schedules`() {
        val weekdays =
            HabitRequest(
                name = "Run",
                schedule = ScheduleDto("weekdays", days = listOf(1, 3, 5)),
            ).validated(today)
        assertEquals(
            Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            weekdays.schedule,
        )
        val weekly = HabitRequest(name = "Gym", schedule = ScheduleDto("weekly", timesPerWeek = 3)).validated(today)
        assertEquals(Schedule.TimesPerWeek(3), weekly.schedule)
    }

    @Test
    fun `reports every invalid field at once`() {
        val fields = errors(HabitRequest(name = " ", color = "blue", target = 0, schedule = null))
        assertEquals(setOf("name", "color", "schedule", "target"), fields.keys)
    }

    @Test
    fun `rejects bad schedules`() {
        assertEquals(setOf("schedule.days"), errors(HabitRequest("Run", schedule = ScheduleDto("weekdays"))).keys)
        assertEquals(
            setOf("schedule.days"),
            errors(HabitRequest("Run", schedule = ScheduleDto("weekdays", days = listOf(1, 8)))).keys,
        )
        assertEquals(
            setOf("schedule.timesPerWeek"),
            errors(HabitRequest("Gym", schedule = ScheduleDto("weekly", timesPerWeek = 8))).keys,
        )
        assertEquals(setOf("schedule.type"), errors(HabitRequest("Gym", schedule = ScheduleDto("monthly"))).keys)
    }

    @Test
    fun `a unit needs a target`() {
        val fields = errors(HabitRequest("Water", schedule = ScheduleDto("daily"), unit = "glasses"))
        assertEquals(setOf("unit"), fields.keys)
    }

    @Test
    fun `start dates can be at most a year ahead`() {
        val fields =
            errors(HabitRequest("Later", schedule = ScheduleDto("daily"), startDate = today.plusYears(1).plusDays(1)))
        assertEquals(setOf("startDate"), fields.keys)
    }

    @Test
    fun `reorder needs every active habit exactly once`() {
        validateReorder(listOf(3, 1, 2), setOf(1, 2, 3))
        assertFailsWith<ValidationException> { validateReorder(listOf(1, 2), setOf(1, 2, 3)) }
        assertFailsWith<ValidationException> { validateReorder(listOf(1, 1, 2, 3), setOf(1, 2, 3)) }
        assertFailsWith<ValidationException> { validateReorder(listOf(1, 2, 4), setOf(1, 2, 3)) }
    }

    @Test
    fun `blank descriptions are stored as null`() {
        val input =
            HabitRequest(
                name = "Stretch",
                description = "   ",
                schedule = ScheduleDto("daily"),
            ).validated(today)
        assertNull(input.description)
    }
}
