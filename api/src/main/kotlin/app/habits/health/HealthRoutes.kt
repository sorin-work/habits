package app.habits.health

import app.habits.db.query
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
)

fun Route.healthRoutes(db: Database) {
    get("/health") {
        val up = runCatching { db.query { exec("SELECT 1") { it.next() } } }.getOrNull() == true
        if (up) {
            call.respond(HealthResponse("ok", "up"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse("unavailable", "down"))
        }
    }
}
