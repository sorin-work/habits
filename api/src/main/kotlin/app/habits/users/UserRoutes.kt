package app.habits.users

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(service: UserService) {
    route("/api/auth") {
        post("/register") {
            val response = service.register(call.receive<RegisterRequest>())
            call.respond(HttpStatusCode.Created, response)
        }
        post("/login") {
            call.respond(service.login(call.receive<LoginRequest>()))
        }
    }
}
