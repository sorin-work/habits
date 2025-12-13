package app.habits.today

import app.habits.auth.AUTH_JWT
import app.habits.auth.userId
import app.habits.users.UserService
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.todayRoutes(
    users: UserService,
    service: TodayService,
) {
    authenticate(AUTH_JWT) {
        get("/api/today") {
            val user = users.get(call.userId)
            call.respond(service.forDay(user))
        }
    }
}
