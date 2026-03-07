package app.habits.users

import app.habits.auth.AUTH_JWT
import app.habits.auth.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

fun Route.userRoutes(service: UserService) {
    authenticate(AUTH_JWT) {
        route("/api/me") {
            get {
                call.respond(service.get(call.userId).toResponse())
            }
            put {
                call.respond(service.updateProfile(call.userId, call.receive()).toResponse())
            }
        }
    }
}
