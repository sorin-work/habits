package app.habits.users

import app.habits.db.Users
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.ZoneId

class UserRepository(
    private val db: Database,
) {
    suspend fun findByEmail(email: String): User? =
        newSuspendedTransaction(Dispatchers.IO, db) {
            Users
                .selectAll()
                .where { Users.email eq email }
                .singleOrNull()
                ?.toUser()
        }

    suspend fun findById(id: Long): User? =
        newSuspendedTransaction(Dispatchers.IO, db) {
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
        newSuspendedTransaction(Dispatchers.IO, db) {
            val id =
                Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = passwordHash
                    it[Users.name] = name
                    it[Users.timeZone] = timeZone.id
                    it[Users.createdAt] = createdAt
                } get Users.id
            User(id, email, passwordHash, name, timeZone, createdAt)
        }
}

private fun ResultRow.toUser() =
    User(
        id = this[Users.id],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        name = this[Users.name],
        timeZone = ZoneId.of(this[Users.timeZone]),
        createdAt = this[Users.createdAt],
    )
