package app.habits.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 254).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val name = varchar("name", 80)
    val timeZone = varchar("time_zone", 64)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
