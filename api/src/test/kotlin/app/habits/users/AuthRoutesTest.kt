package app.habits.users

import app.habits.apiTest
import app.habits.obj
import app.habits.str
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `register returns a token and the profile`() =
        apiTest {
            val response =
                post(
                    "/api/auth/register",
                    mapOf(
                        "email" to "Sam@Example.com",
                        "password" to "correct-horse",
                        "name" to "Sam",
                        "timeZone" to "Europe/Berlin",
                    ),
                )
            assertEquals(HttpStatusCode.Created, response.status)
            val body = response.obj()
            assertTrue(body["token"]!!.str.isNotBlank())
            val user = body["user"]!!.jsonObject
            assertEquals("sam@example.com", user["email"]!!.str)
            assertEquals("Europe/Berlin", user["timeZone"]!!.str)
        }

    @Test
    fun `register validates every field`() =
        apiTest {
            val response =
                post(
                    "/api/auth/register",
                    mapOf(
                        "email" to "nope",
                        "password" to "short",
                        "name" to "",
                        "timeZone" to "Moon/Base",
                    ),
                )
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val body = response.obj()
            assertEquals("validation_failed", body["error"]!!.str)
            val fields = body["fields"]!!.jsonObject
            assertEquals(setOf("email", "password", "name", "timeZone"), fields.keys)
        }

    @Test
    fun `an email can only be registered once, whatever its case`() =
        apiTest {
            register("sam@example.com")
            val response =
                post(
                    "/api/auth/register",
                    mapOf(
                        "email" to "SAM@example.com",
                        "password" to "correct-horse",
                        "name" to "Sam",
                    ),
                )
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals("email_taken", response.obj()["error"]!!.str)
        }

    @Test
    fun `login rejects a wrong password and unknown emails the same way`() =
        apiTest {
            register("sam@example.com")
            val wrong = post("/api/auth/login", mapOf("email" to "sam@example.com", "password" to "wrong-horse"))
            val unknown = post("/api/auth/login", mapOf("email" to "kim@example.com", "password" to "correct-horse"))
            assertEquals(HttpStatusCode.Unauthorized, wrong.status)
            assertEquals(wrong.bodyAsText(), unknown.bodyAsText())
        }

    @Test
    fun `malformed bodies get a json error`() =
        apiTest {
            val response =
                client.post("/api/auth/login") {
                    headers.append(HttpHeaders.ContentType, "application/json")
                    setBody("{not json")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("invalid_body", response.obj()["error"]!!.str)
        }

    @Test
    fun `profile endpoints need a token`() =
        apiTest {
            val response = get("/api/me")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("unauthorized", response.obj()["error"]!!.str)
            assertEquals(HttpStatusCode.Unauthorized, get("/api/me", token = "not-a-token").status)
        }
}
