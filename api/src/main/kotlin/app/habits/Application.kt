package app.habits

import app.habits.auth.JwtService
import app.habits.auth.PasswordHasher
import app.habits.auth.configureSecurity
import app.habits.checkins.CheckInRepository
import app.habits.common.AppJson
import app.habits.common.ErrorResponse
import app.habits.common.configureErrorHandling
import app.habits.config.AppConfig
import app.habits.db.DatabaseFactory
import app.habits.habits.HabitRepository
import app.habits.habits.HabitService
import app.habits.habits.habitRoutes
import app.habits.health.healthRoutes
import app.habits.seed.DemoSeeder
import app.habits.stats.StatsService
import app.habits.today.TodayService
import app.habits.today.todayRoutes
import app.habits.users.AUTH_RATE_LIMIT
import app.habits.users.UserRepository
import app.habits.users.UserService
import app.habits.users.authRoutes
import app.habits.users.userRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.net.URI
import java.time.Clock
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes

fun main() {
    // Timestamps are stored without a zone; keep the JVM on UTC so they always mean UTC.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

fun Application.module(
    config: AppConfig,
    clock: Clock = Clock.systemUTC(),
) {
    val dataSource = DatabaseFactory.dataSource(config.database)
    DatabaseFactory.migrate(dataSource)
    val db = DatabaseFactory.connect(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }

    val hasher = PasswordHasher(config.bcryptCost)
    val jwt = JwtService(config.jwt, clock)
    val userRepository = UserRepository(db)
    val habitRepository = HabitRepository(db)
    val checkInRepository = CheckInRepository(db)
    val userService = UserService(userRepository, hasher, jwt, clock)
    val habitService = HabitService(habitRepository, checkInRepository, clock)
    val statsService = StatsService(habitService, habitRepository, checkInRepository, clock)
    val todayService = TodayService(habitRepository, checkInRepository, clock)

    install(ContentNegotiation) { json(AppJson) }
    install(CallLogging) {
        level = Level.INFO
    }
    install(CORS) {
        config.corsAllowedOrigins.forEach { origin ->
            val uri = URI(origin)
            val host = if (uri.port == -1) uri.host else "${uri.host}:${uri.port}"
            allowHost(host, schemes = listOf(uri.scheme))
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = config.loginRateLimit, refillPeriod = 1.minutes)
            requestKey { it.request.origin.remoteHost }
        }
    }
    configureErrorHandling()
    configureSecurity(jwt)

    routing {
        healthRoutes(db)
        authRoutes(userService)
        userRoutes(userService)
        habitRoutes(userService, habitService, statsService, clock)
        todayRoutes(userService, todayService)
    }

    if (config.seedDemo) {
        runBlocking { DemoSeeder(userRepository, habitRepository, checkInRepository, hasher, clock).seed() }
    }
}
