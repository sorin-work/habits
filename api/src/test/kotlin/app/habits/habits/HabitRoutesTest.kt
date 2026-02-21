package app.habits.habits

import app.habits.apiTest
import app.habits.arr
import app.habits.dailyHabit
import app.habits.int
import app.habits.obj
import app.habits.str
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
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
            assertEquals(listOf(0, 1), list.map { it.jsonObject["position"]!!.int })
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
    fun `archived habits leave the list and can be restored at the end`() =
        apiTest {
            val token = register()
            val first = createHabit(token, dailyHabit("First"))
            createHabit(token, dailyHabit("Second"))

            val archived = post("/api/habits/$first/archive", token = token)
            assertEquals(HttpStatusCode.OK, archived.status)
            assertEquals(
                true,
                archived
                    .obj()["archived"]!!
                    .jsonPrimitive.content
                    .toBoolean(),
            )
            assertEquals(listOf("Second"), get("/api/habits", token).arr().map { it.jsonObject["name"]!!.str })
            assertEquals(
                listOf("First"),
                get("/api/habits?archived=true", token).arr().map { it.jsonObject["name"]!!.str },
            )

            assertEquals(HttpStatusCode.OK, post("/api/habits/$first/restore", token = token).status)
            assertEquals(listOf("Second", "First"), get("/api/habits", token).arr().map { it.jsonObject["name"]!!.str })
        }

    @Test
    fun `reorder habits`() =
        apiTest {
            val token = register()
            val a = createHabit(token, dailyHabit("A"))
            val b = createHabit(token, dailyHabit("B"))
            val c = createHabit(token, dailyHabit("C"))

            val response = put("/api/habits/order", mapOf("ids" to listOf(c, a, b)), token)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf("C", "A", "B"), response.arr().map { it.jsonObject["name"]!!.str })
            assertEquals(listOf("C", "A", "B"), get("/api/habits", token).arr().map { it.jsonObject["name"]!!.str })
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

    @Test
    fun `the web dev server may send put requests`() =
        apiTest {
            val response =
                client.options("/api/habits/1") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    header(HttpHeaders.AccessControlRequestMethod, "PUT")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.headers[HttpHeaders.AccessControlAllowMethods].orEmpty().contains("PUT"))
        }
}
