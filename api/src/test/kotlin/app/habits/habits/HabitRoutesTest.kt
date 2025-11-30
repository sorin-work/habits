package app.habits.habits

import app.habits.apiTest
import app.habits.arr
import app.habits.dailyHabit
import app.habits.int
import app.habits.obj
import app.habits.str
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HabitRoutesTest {
    @Test
    fun `create and list habits`() =
        apiTest {
            val token = register()
            val response =
                post(
                    "/api/habits",
                    mapOf(
                        "name" to "Run",
                        "color" to "#EF6C57",
                        "schedule" to mapOf("type" to "weekdays", "days" to listOf(5, 1, 3)),
                        "target" to 5,
                        "unit" to "km",
                    ),
                    token,
                )
            assertEquals(HttpStatusCode.Created, response.status)
            val created = response.obj()
            assertEquals("#ef6c57", created["color"]!!.str)
            assertEquals("2026-03-18", created["startDate"]!!.str)
            val days = (created["schedule"] as JsonObject)["days"]!!.jsonArray
            assertEquals(listOf(1, 3, 5), days.map { it.int })

            createHabit(token, dailyHabit("Read"))
            val list = get("/api/habits", token).arr()
            assertEquals(listOf("Run", "Read"), list.map { it.jsonObject["name"]!!.str })
        }

    @Test
    fun `invalid habits are rejected with field errors`() =
        apiTest {
            val token = register()
            val response = post("/api/habits", mapOf("name" to "", "schedule" to mapOf("type" to "weekly")), token)
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val fields = response.obj()["fields"] as JsonObject
            assertEquals(setOf("name", "schedule.timesPerWeek"), fields.keys)
        }

    @Test
    fun `habits are private to their owner`() =
        apiTest {
            val sam = register("sam@example.com")
            val kim = register("kim@example.com")
            val id = createHabit(sam)
            assertEquals(HttpStatusCode.NotFound, get("/api/habits/$id", kim).status)
            assertEquals(HttpStatusCode.NotFound, put("/api/habits/$id", dailyHabit("Hijacked"), kim).status)
            assertEquals(HttpStatusCode.NotFound, delete("/api/habits/$id", token = kim).status)
            assertTrue(get("/api/habits", kim).arr().isEmpty())
        }

    @Test
    fun `update keeps the start date unless a new one is given`() =
        apiTest {
            val token = register()
            val id = createHabit(token, dailyHabit("Read", startDate = "2026-02-01"))
            val response =
                put(
                    "/api/habits/$id",
                    mapOf(
                        "name" to "Read more",
                        "schedule" to mapOf("type" to "daily"),
                        "target" to 30,
                        "unit" to "pages",
                    ),
                    token,
                )
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.obj()
            assertEquals("Read more", body["name"]!!.str)
            assertEquals("2026-02-01", body["startDate"]!!.str)
            assertEquals("pages", body["unit"]!!.str)
        }

    @Test
    fun `delete removes the habit`() =
        apiTest {
            val token = register()
            val id = createHabit(token)
            assertEquals(HttpStatusCode.NoContent, delete("/api/habits/$id", token = token).status)
            assertEquals(HttpStatusCode.NotFound, get("/api/habits/$id", token).status)
            assertEquals(HttpStatusCode.NotFound, get("/api/habits/not-a-number", token).status)
        }
}
