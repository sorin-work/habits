package app.habits.today

import app.habits.apiTest
import app.habits.dailyHabit
import app.habits.int
import app.habits.obj
import app.habits.str
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class TodayRoutesTest {
    private fun JsonObject.byName(name: String) =
        (this["habits"] as JsonArray).map { it.jsonObject }.first { (it["habit"] as JsonObject)["name"]!!.str == name }

    private val JsonObject.due get() = this["due"]!!.jsonPrimitive.content.toBoolean()

    @Test
    fun `shows which habits are due today`() =
        apiTest {
            val token = register()
            createHabit(token, dailyHabit("Read"))
            // Wednesday the 18th: a Tue/Thu habit is not due.
            createHabit(
                token,
                mapOf(
                    "name" to "Swim",
                    "schedule" to mapOf("type" to "weekdays", "days" to listOf(2, 4)),
                    "startDate" to "2026-03-01",
                ),
            )
            val gym =
                createHabit(
                    token,
                    mapOf(
                        "name" to "Gym",
                        "schedule" to mapOf("type" to "weekly", "timesPerWeek" to 2),
                        "startDate" to "2026-03-01",
                    ),
                )

            var body = get("/api/today", token).obj()
            assertEquals("2026-03-18", body["date"]!!.str)
            assertEquals(true, body.byName("Read").due)
            assertEquals(false, body.byName("Swim").due)
            assertEquals(true, body.byName("Gym").due)
            assertEquals(2, body["dueCount"]!!.int)

            // Once the weekly goal is met on other days it's no longer due today.
            put("/api/habits/$gym/checkins/2026-03-16", mapOf<String, Any>(), token)
            put("/api/habits/$gym/checkins/2026-03-17", mapOf<String, Any>(), token)
            body = get("/api/today", token).obj()
            assertEquals(false, body.byName("Gym").due)
            val week = body.byName("Gym")["week"] as JsonObject
            assertEquals(2, week["done"]!!.int)
            assertEquals(2, week["goal"]!!.int)
        }

    @Test
    fun `includes the day's check-in and streak`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Water", target = 8))
            put("/api/habits/$id/checkins/2026-03-17", mapOf("value" to 8), token)
            put("/api/habits/$id/checkins/2026-03-18", mapOf("value" to 3, "note" to "slow start"), token)
            val water = get("/api/today", token).obj().byName("Water")
            assertEquals(3, water["value"]!!.int)
            assertEquals("slow start", water["note"]!!.str)
            assertEquals(false, water["completed"]!!.jsonPrimitive.content.toBoolean())
            assertEquals(1, (water["streak"] as JsonObject)["current"]!!.int)
        }

    @Test
    fun `leaves out archived habits and ones that haven't started`() =
        apiTest {
            val token = register()
            val archived = createHabit(token, dailyHabit("Old"))
            post("/api/habits/$archived/archive", token = token)
            createHabit(token, dailyHabit("Later", startDate = "2026-04-01"))
            createHabit(token, dailyHabit("Now"))
            val habits = get("/api/today", token).obj()["habits"] as JsonArray
            assertEquals(listOf("Now"), habits.map { (it.jsonObject["habit"] as JsonObject)["name"]!!.str })
        }

    @Test
    fun `days follow the user's time zone`() =
        apiTest {
            // 10:00 UTC on the 18th is already the 19th on Kiritimati (UTC+14).
            val token = register(timeZone = "Pacific/Kiritimati")
            val id = createHabit(token, dailyHabit("Read"))
            assertEquals("2026-03-19", get("/api/today", token).obj()["date"]!!.str)
            assertEquals(
                HttpStatusCode.OK,
                put("/api/habits/$id/checkins/2026-03-19", mapOf<String, Any>(), token).status,
            )

            val utc = register("kim@example.com")
            val other = createHabit(utc, dailyHabit("Read"))
            assertEquals(
                HttpStatusCode.BadRequest,
                put("/api/habits/$other/checkins/2026-03-19", mapOf<String, Any>(), utc).status,
            )
        }

    @Test
    fun `can look at a past day but not a future one`() =
        apiTest {
            val token = register()
            createHabit(token, dailyHabit("Read"))
            val past = get("/api/today?date=2026-03-10", token).obj()
            assertEquals("2026-03-10", past["date"]!!.str)
            assertEquals("2026-03-18", past["today"]!!.str)
            assertEquals(HttpStatusCode.BadRequest, get("/api/today?date=2026-03-19", token).status)
        }
}
