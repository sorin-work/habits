package app.habits

import app.habits.seed.DEMO_EMAIL
import app.habits.seed.DEMO_PASSWORD
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthAndSeedTest {
    @Test
    fun `health reports the database`() =
        apiTest {
            val response = get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("up", response.obj()["database"]!!.str)
        }

    @Test
    fun `unknown endpoints get a json 404`() =
        apiTest {
            val response = get("/api/nope")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("not_found", response.obj()["error"]!!.str)
        }

    @Test
    fun `demo seed creates a usable account`() =
        apiTest(seedDemo = true) {
            val login = post("/api/auth/login", mapOf("email" to DEMO_EMAIL, "password" to DEMO_PASSWORD))
            assertEquals(HttpStatusCode.OK, login.status)
            val token = login.obj()["token"]!!.str

            val habits = get("/api/habits", token).arr()
            assertEquals(7, habits.size)
            assertEquals(1, get("/api/habits?archived=true", token).arr().size)

            val today = get("/api/today", token).obj()
            assertTrue((today["habits"] as JsonArray).isNotEmpty())
            val meditate =
                (today["habits"] as JsonArray).map { it.jsonObject }.first {
                    it["habit"]!!.jsonObject["name"]!!.str ==
                        "Meditate"
                }
            assertTrue(meditate["streak"]!!.jsonObject["current"]!!.int >= 12)
        }
}
