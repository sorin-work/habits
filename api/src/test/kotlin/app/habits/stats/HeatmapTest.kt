package app.habits.stats

import app.habits.habits.Habit
import app.habits.habits.Schedule
import java.time.DayOfWeek.MONDAY
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HeatmapTest {
    private fun habit(
        target: Int?,
        schedule: Schedule = Schedule.Daily,
    ) = Habit(
        id = 1,
        userId = 1,
        name = "Water",
        description = null,
        color = "#3fa7d6",
        schedule = schedule,
        target = target,
        unit = null,
        startDate = LocalDate.of(2026, 3, 10),
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `levels scale with progress towards the goal`() {
        assertEquals(0, Heatmap.level(0, 8))
        assertEquals(1, Heatmap.level(1, 8))
        assertEquals(2, Heatmap.level(5, 8))
        assertEquals(3, Heatmap.level(7, 8))
        assertEquals(4, Heatmap.level(8, 8))
        assertEquals(4, Heatmap.level(12, 8))
        assertEquals(4, Heatmap.level(1, 1))
    }

    @Test
    fun `builds one entry per day with schedule and completion`() {
        val days =
            Heatmap.build(
                habit(target = 8, schedule = Schedule.Weekdays(setOf(MONDAY))),
                mapOf(LocalDate.of(2026, 3, 16) to 8, LocalDate.of(2026, 3, 17) to 3),
                LocalDate.of(2026, 3, 8),
                LocalDate.of(2026, 3, 17),
            )
        assertEquals(10, days.size)
        // Monday the 9th is before the start date.
        assertEquals(false, days.first { it.date == LocalDate.of(2026, 3, 9) }.scheduled)
        val monday = days.first { it.date == LocalDate.of(2026, 3, 16) }
        assertEquals(true, monday.scheduled)
        assertEquals(true, monday.completed)
        val tuesday = days.last()
        assertEquals(false, tuesday.completed)
        assertEquals(2, tuesday.level)
    }
}
