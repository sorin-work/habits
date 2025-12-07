package app.habits.checkins

import app.habits.apiTest
import app.habits.arr
import app.habits.dailyHabit
import app.habits.int
import app.habits.obj
import app.habits.str
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckInRoutesTest {
    @Test
    fun `partial progress is stored but not complete`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Water", target = 8))
            val response = put("/api/habits/$id/checkins/2026-03-17", mapOf("value" to 5), token)
            assertEquals(
                false,
                response
                    .obj()["completed"]!!
                    .jsonPrimitive.content
                    .toBoolean(),
            )
            // Updating the same day replaces the value.
            val updated = put("/api/habits/$id/checkins/2026-03-17", mapOf("value" to 9), token)
            assertEquals(
                true,
                updated
                    .obj()["completed"]!!
                    .jsonPrimitive.content
                    .toBoolean(),
            )
            assertEquals(1, get("/api/habits/$id/checkins", token).arr().size)
        }

    @Test
    fun `future days and days before the start are rejected`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit(startDate = "2026-03-10"))
            val future = put("/api/habits/$id/checkins/2026-03-19", mapOf<String, Any>(), token)
            assertEquals(HttpStatusCode.BadRequest, future.status)
            assertEquals("future_date", future.obj()["error"]!!.str)
            val early = put("/api/habits/$id/checkins/2026-03-09", mapOf<String, Any>(), token)
            assertEquals("before_start", early.obj()["error"]!!.str)
            val bad = put("/api/habits/$id/checkins/yesterday", mapOf<String, Any>(), token)
            assertEquals("invalid_date", bad.obj()["error"]!!.str)
        }

    @Test
    fun `values and notes are validated`() =
        apiTest {
            val token = register()
            val id = createHabit(token)
            val response =
                put("/api/habits/$id/checkins/2026-03-18", mapOf("value" to 0, "note" to "x".repeat(501)), token)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(setOf("value", "note"), response.obj()["fields"]!!.jsonObject.keys)
        }

    @Test
    fun `undo a check-in`() =
        apiTest {
            val token = register()
            val id = createHabit(token)
            put("/api/habits/$id/checkins/2026-03-18", mapOf<String, Any>(), token)
            assertEquals(HttpStatusCode.NoContent, delete("/api/habits/$id/checkins/2026-03-18", token = token).status)
            assertEquals(HttpStatusCode.NotFound, delete("/api/habits/$id/checkins/2026-03-18", token = token).status)
        }

    @Test
    fun `list check-ins newest first within a range`() =
        apiTest {
            val token = register()
            val id = createHabit(token)
            for (day in listOf("2026-03-02", "2026-03-10", "2026-03-15")) {
                put("/api/habits/$id/checkins/$day", mapOf("note" to "on $day"), token)
            }
            val all = get("/api/habits/$id/checkins", token).arr()
            assertEquals(listOf("2026-03-15", "2026-03-10", "2026-03-02"), all.map { it.jsonObject["date"]!!.str })
            val some = get("/api/habits/$id/checkins?from=2026-03-05&to=2026-03-12", token).arr()
            assertEquals(listOf("on 2026-03-10"), some.map { it.jsonObject["note"]!!.str })
            assertEquals(
                HttpStatusCode.BadRequest,
                get("/api/habits/$id/checkins?from=2026-03-12&to=2026-03-05", token).status,
            )
        }

    @Test
    fun `future days are decided in the user's time zone`() =
        apiTest {
            // 10:00 UTC on the 18th is already the 19th in Kiritimati (UTC+14).
            val token = register(timeZone = "Pacific/Kiritimati")
            val id = createHabit(token)
            assertEquals(
                HttpStatusCode.OK,
                put("/api/habits/$id/checkins/2026-03-19", mapOf<String, Any>(), token).status,
            )
            val tooEarly = put("/api/habits/$id/checkins/2026-03-20", mapOf<String, Any>(), token)
            assertEquals(HttpStatusCode.BadRequest, tooEarly.status)
        }
}
