package app.habits.stats

import app.habits.apiTest
import app.habits.dailyHabit
import app.habits.int
import app.habits.obj
import app.habits.str
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsRoutesTest {
    @Test
    fun `habit detail includes streaks and totals`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Water", target = 8))
            for ((day, value) in listOf(14 to 8, 15 to 3, 16 to 8, 17 to 9, 18 to 10)) {
                put("/api/habits/$id/checkins/2026-03-$day", mapOf("value" to value), token)
            }
            val body = get("/api/habits/$id", token).obj()
            assertEquals("Water", (body["habit"] as JsonObject)["name"]!!.str)
            val summary = body["summary"] as JsonObject
            val streak = summary["streak"] as JsonObject
            assertEquals(3, streak["current"]!!.int)
            assertEquals(3, streak["best"]!!.int)
            assertEquals("days", streak["unit"]!!.str)
            assertEquals(5, summary["totalCheckIns"]!!.int)
            // Started on the 1st: 18 days expected, 4 completed.
            val last30 = summary["last30Days"] as JsonObject
            assertEquals(18, last30["expected"]!!.int)
            assertEquals(4, last30["completed"]!!.int)
        }

    @Test
    fun `heatmap covers the requested range`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Water", target = 4))
            put("/api/habits/$id/checkins/2026-03-10", mapOf("value" to 2), token)
            put("/api/habits/$id/checkins/2026-03-11", mapOf("value" to 4), token)
            val body = get("/api/habits/$id/heatmap?from=2026-03-09&to=2026-03-15", token).obj()
            assertEquals(4, body["goal"]!!.int)
            val days = body["days"]!!.jsonArray
            assertEquals(7, days.size)
            assertEquals(listOf(0, 2, 4, 0, 0, 0, 0), days.map { it.jsonObject["level"]!!.int })

            val year = get("/api/habits/$id/heatmap", token).obj()
            assertEquals("2025-03-19", year["from"]!!.str)
            assertEquals("2026-03-18", year["to"]!!.str)
            assertEquals(365, year["days"]!!.jsonArray.size)

            assertEquals(
                HttpStatusCode.BadRequest,
                get("/api/habits/$id/heatmap?from=2020-01-01&to=2026-01-01", token).status,
            )
        }

    @Test
    fun `weekly and monthly stats`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Read"))
            val days = listOf(2, 3, 4, 9, 16, 17)
            for (day in days) put("/api/habits/$id/checkins/2026-03-%02d".format(day), mapOf<String, Any>(), token)

            val weeks = get("/api/habits/$id/stats?period=week&count=3", token).obj()
            val buckets = weeks["buckets"] as JsonArray
            assertEquals(listOf("2026-03-02", "2026-03-09", "2026-03-16"), buckets.map { it.jsonObject["start"]!!.str })
            assertEquals(listOf(3, 1, 2), buckets.map { it.jsonObject["completed"]!!.int })

            val months = get("/api/habits/$id/stats?period=month&count=2", token).obj()["buckets"] as JsonArray

            assertEquals(HttpStatusCode.BadRequest, get("/api/habits/$id/stats?period=year", token).status)
            assertEquals(HttpStatusCode.BadRequest, get("/api/habits/$id/stats?count=500", token).status)
        }
}
