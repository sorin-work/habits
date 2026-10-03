package app.habits

import app.habits.common.AppJson
import app.habits.config.AppConfig
import app.habits.config.DatabaseConfig
import app.habits.config.JwtConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID
import kotlin.test.assertEquals

/** Wednesday 18 March 2026, 10:00 UTC. */
val TEST_NOW: Instant = Instant.parse("2026-03-18T10:00:00Z")
val testClock: Clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)

fun testConfig(seedDemo: Boolean = false) =
    AppConfig(
        port = 0,
        database =
            DatabaseConfig(
                url =
                    "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;" +
                        "DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=DAY,VALUE;DB_CLOSE_DELAY=-1",
                user = "sa",
                password = "",
                maxPoolSize = 2,
            ),
        jwt = JwtConfig(secret = "test-secret-that-is-long-enough-for-hmac-256"),
        corsAllowedOrigins = listOf("http://localhost:5173"),
        seedDemo = seedDemo,
        bcryptCost = 4,
        loginRateLimit = 1000,
    )

class Api(
    val client: HttpClient,
) {
    suspend fun get(
        path: String,
        token: String? = null,
    ) = client.get(path) { auth(token) }

    suspend fun post(
        path: String,
        body: Any? = null,
        token: String? = null,
    ) = client.post(path) {
        auth(token)
        json(body)
    }

    suspend fun put(
        path: String,
        body: Any? = null,
        token: String? = null,
    ) = client.put(path) {
        auth(token)
        json(body)
    }

    suspend fun delete(
        path: String,
        body: Any? = null,
        token: String? = null,
    ) = client.delete(path) {
        auth(token)
        json(body)
    }

    /** Registers a user and returns their token. */
    suspend fun register(
        email: String = "sam@example.com",
        password: String = "correct-horse",
        timeZone: String = "UTC",
    ): String {
        val response =
            post(
                "/api/auth/register",
                mapOf(
                    "email" to email,
                    "password" to password,
                    "name" to "Sam",
                    "timeZone" to timeZone,
                ),
            )
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return response.obj()["token"]!!.jsonPrimitive.content
    }

    suspend fun createHabit(
        token: String,
        body: Map<String, Any?> = dailyHabit(),
    ): Long {
        val response = post("/api/habits", body, token)
        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        return response
            .obj()["id"]!!
            .jsonPrimitive.content
            .toLong()
    }

    private fun HttpRequestBuilder.auth(token: String?) {
        if (token != null) bearerAuth(token)
    }

    private fun HttpRequestBuilder.json(body: Any?) {
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(toJson(body).toString())
        }
    }
}

fun dailyHabit(
    name: String = "Read",
    target: Int? = null,
    startDate: String = "2026-03-01",
) = mapOf(
    "name" to name,
    "schedule" to mapOf("type" to "daily"),
    "target" to target,
    "startDate" to startDate,
)

fun apiTest(
    clock: Clock = testClock,
    seedDemo: Boolean = false,
    block: suspend Api.(ApplicationTestBuilder) -> Unit,
) = testApplication {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    application { module(testConfig(seedDemo), clock) }
    val client = createClient { install(ContentNegotiation) { json(AppJson) } }
    Api(client).block(this)
}

suspend fun HttpResponse.obj(): JsonObject = AppJson.parseToJsonElement(bodyAsText()).jsonObject

suspend fun HttpResponse.arr(): JsonArray = AppJson.parseToJsonElement(bodyAsText()).jsonArray

val JsonElement.str get() = jsonPrimitive.content
val JsonElement.int get() = jsonPrimitive.content.toInt()

@Suppress("UNCHECKED_CAST")
private fun toJson(value: Any?): JsonElement =
    when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject((value as Map<String, Any?>).mapValues { toJson(it.value) })
        is List<*> -> JsonArray(value.map { toJson(it) })
        else -> error("Can't encode ${value::class}")
    }
