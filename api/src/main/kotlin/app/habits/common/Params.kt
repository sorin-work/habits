package app.habits.common

import io.ktor.server.application.ApplicationCall

fun ApplicationCall.longPath(name: String): Long =
    parameters[name]?.toLongOrNull() ?: throw NotFoundException("Resource")
