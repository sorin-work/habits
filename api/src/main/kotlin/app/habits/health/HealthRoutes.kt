package app.habits.health

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
)

fun Route.healthRoutes(db: Database) {
    get("/health") {
        newSuspendedTransaction(Dispatchers.IO, db) { exec("SELECT 1") { it.next() } }
        call.respond(HealthResponse("ok", "up"))
    }
}
