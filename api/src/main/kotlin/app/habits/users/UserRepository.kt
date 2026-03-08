package app.habits.users

import app.habits.db.Users
import app.habits.db.query
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class UserRepository(
    private val db: Database,
) {
    suspend fun findByEmail(email: String): User? =
        db.query {
            Users
                .selectAll()
                .where { Users.email eq email }
                .singleOrNull()
                ?.toUser()
        }

    suspend fun findById(id: Long): User? =
        db.query {
            Users
                .selectAll()
                .where { Users.id eq id }
                .singleOrNull()
                ?.toUser()
        }

    suspend fun create(
        email: String,
        passwordHash: String,
        name: String,
        timeZone: ZoneId,
        createdAt: Instant,
    ): User =
        db.query {
            val id =
                Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = passwordHash
                    it[Users.name] = name
                    it[Users.timeZone] = timeZone.id
                    it[Users.weekStart] = DayOfWeek.MONDAY.name
                    it[Users.createdAt] = createdAt
                } get Users.id
            User(id, email, passwordHash, name, timeZone, DayOfWeek.MONDAY, createdAt)
        }

    suspend fun updateProfile(
        id: Long,
        name: String,
        timeZone: ZoneId,
        weekStart: DayOfWeek,
    ) = db.query {
        Users.update({ Users.id eq id }) {
            it[Users.name] = name
            it[Users.timeZone] = timeZone.id
            it[Users.weekStart] = weekStart.name
        }
    }

    suspend fun updatePassword(
        id: Long,
        passwordHash: String,
    ) = db.query {
        Users.update({ Users.id eq id }) { it[Users.passwordHash] = passwordHash }
    }
}

private fun ResultRow.toUser() =
    User(
        id = this[Users.id],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        name = this[Users.name],
        timeZone = ZoneId.of(this[Users.timeZone]),
        weekStart = DayOfWeek.valueOf(this[Users.weekStart]),
        createdAt = this[Users.createdAt],
    )
