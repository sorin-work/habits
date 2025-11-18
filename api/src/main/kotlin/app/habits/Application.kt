package app.habits

import app.habits.common.AppJson
import app.habits.common.configureErrorHandling
import app.habits.config.AppConfig
import app.habits.db.DatabaseFactory
import app.habits.health.healthRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import java.util.TimeZone

fun main() {
    // Timestamps are stored without a zone; keep the JVM on UTC so they always mean UTC.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

fun Application.module(config: AppConfig) {
    val dataSource = DatabaseFactory.dataSource(config.database)
    DatabaseFactory.migrate(dataSource)
    val db = DatabaseFactory.connect(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }

    install(ContentNegotiation) { json(AppJson) }
    configureErrorHandling()

    routing {
        healthRoutes(db)
    }
}
