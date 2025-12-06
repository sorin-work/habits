package app.habits.habits

import app.habits.auth.AUTH_JWT
import app.habits.auth.userId
import app.habits.checkins.CheckInResponse
import app.habits.common.datePath
import app.habits.common.longPath
import app.habits.users.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.habitRoutes(
    users: UserService,
    service: HabitService,
) {
    authenticate(AUTH_JWT) {
        route("/api/habits") {
            get {
                val user = users.get(call.userId)
                call.respond(service.list(user).map { it.toResponse() })
            }
            post {
                val user = users.get(call.userId)
                val habit = service.create(user, call.receive())
                call.respond(HttpStatusCode.Created, habit.toResponse())
            }

            route("/{id}") {
                get {
                    val user = users.get(call.userId)
                    call.respond(service.get(user, call.longPath("id")).toResponse())
                }
                put {
                    val user = users.get(call.userId)
                    call.respond(service.update(user, call.longPath("id"), call.receive()).toResponse())
                }
                delete {
                    val user = users.get(call.userId)
                    service.delete(user, call.longPath("id"))
                    call.respond(HttpStatusCode.NoContent)
                }

                put("/checkins/{date}") {
                    val user = users.get(call.userId)
                    val (habit, entry) =
                        service.checkIn(
                            user,
                            call.longPath("id"),
                            call.datePath("date"),
                            call.receive(),
                        )
                    call.respond(
                        CheckInResponse(
                            entry.day,
                            entry.value,
                            entry.note,
                            entry.updatedAt,
                        ),
                    )
                }
                delete("/checkins/{date}") {
                    val user = users.get(call.userId)
                    service.undoCheckIn(user, call.longPath("id"), call.datePath("date"))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
