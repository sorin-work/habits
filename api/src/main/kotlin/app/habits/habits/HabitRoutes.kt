package app.habits.habits

import app.habits.auth.AUTH_JWT
import app.habits.auth.userId
import app.habits.checkins.CheckInResponse
import app.habits.common.ApiException
import app.habits.common.datePath
import app.habits.common.dateRange
import app.habits.common.intQuery
import app.habits.common.longPath
import app.habits.common.today
import app.habits.stats.StatsPeriod
import app.habits.stats.StatsService
import app.habits.users.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.Clock

fun Route.habitRoutes(
    users: UserService,
    service: HabitService,
    stats: StatsService,
    clock: Clock,
) {
    authenticate(AUTH_JWT) {
        route("/api/habits") {
            get {
                val user = users.get(call.userId)
                val archived = call.request.queryParameters["archived"] == "true"
                call.respond(service.list(user, archived).map { it.toResponse() })
            }
            post {
                val user = users.get(call.userId)
                val habit = service.create(user, call.receive())
                call.respond(HttpStatusCode.Created, habit.toResponse())
            }

            route("/{id}") {
                get {
                    val user = users.get(call.userId)
                    call.respond(stats.detail(user, call.longPath("id")))
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
                post("/archive") {
                    val user = users.get(call.userId)
                    call.respond(service.archive(user, call.longPath("id")).toResponse())
                }
                post("/restore") {
                    val user = users.get(call.userId)
                    call.respond(service.restore(user, call.longPath("id")).toResponse())
                }

                get("/checkins") {
                    val user = users.get(call.userId)
                    val today = clock.today(user.timeZone)
                    val range = call.dateRange(today.minusDays(89), today)
                    val (habit, entries) = service.checkIns(user, call.longPath("id"), range)
                    call.respond(
                        entries.map {
                            CheckInResponse(
                                it.day,
                                it.value,
                                it.note,
                                habit.isComplete(it.value),
                                it.updatedAt,
                            )
                        },
                    )
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
                            habit.isComplete(entry.value),
                            entry.updatedAt,
                        ),
                    )
                }
                delete("/checkins/{date}") {
                    val user = users.get(call.userId)
                    service.undoCheckIn(user, call.longPath("id"), call.datePath("date"))
                    call.respond(HttpStatusCode.NoContent)
                }

                get("/stats") {
                    val user = users.get(call.userId)
                    val period = call.statsPeriod()
                    val count = call.intQuery("count", default = 12, range = 1..104)
                    call.respond(stats.stats(user, call.longPath("id"), period, count))
                }
                get("/heatmap") {
                    val user = users.get(call.userId)
                    val today = clock.today(user.timeZone)
                    val range = call.dateRange(today.minusDays(364), today)
                    call.respond(stats.heatmap(user, call.longPath("id"), range))
                }
            }
        }

        get("/api/stats/overview") {
            val user = users.get(call.userId)
            val period = call.statsPeriod()
            val count = call.intQuery("count", default = 12, range = 1..104)
            call.respond(stats.overview(user, period, count))
        }
    }
}

private fun ApplicationCall.statsPeriod(): StatsPeriod =
    when (request.queryParameters["period"] ?: "week") {
        "week" -> StatsPeriod.WEEK
        "month" -> StatsPeriod.MONTH
        else -> throw ApiException(HttpStatusCode.BadRequest, "invalid_parameter", "'period' must be week or month")
    }
