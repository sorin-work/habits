package app.habits.habits

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleTest {
    @Test
    fun `round trips through database columns`() {
        val schedules =
            listOf(
                Schedule.Daily,
                Schedule.Weekdays(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)),
                Schedule.TimesPerWeek(4),
            )
        for (schedule in schedules) {
            val columns = schedule.toColumns()
            assertEquals(schedule, scheduleFromColumns(columns.type, columns.days, columns.timesPerWeek))
        }
        assertEquals("1,7", Schedule.Weekdays(setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)).toColumns().days)
    }

    @Test
    fun `weekday schedules know their days`() {
        val schedule = Schedule.Weekdays(setOf(DayOfWeek.SATURDAY))
        assertTrue(schedule.isScheduledOn(LocalDate.of(2026, 3, 21)))
        assertFalse(schedule.isScheduledOn(LocalDate.of(2026, 3, 20)))
        assertTrue(Schedule.TimesPerWeek(1).isScheduledOn(LocalDate.of(2026, 3, 20)))
    }

    @Test
    fun `dto lists days in order`() {
        val dto = Schedule.Weekdays(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)).toDto()
        assertEquals(ScheduleDto("weekdays", days = listOf(1, 5)), dto)
    }
}
