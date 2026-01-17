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
}
