package app.habits.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
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

object Habits : Table("habits") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 80)
    val description = varchar("description", 500).nullable()
    val color = varchar("color", 7)
    val scheduleType = varchar("schedule_type", 16)
    val scheduleDays = varchar("schedule_days", 20).nullable()
    val timesPerWeek = integer("times_per_week").nullable()
    val target = integer("target").nullable()
    val unit = varchar("unit", 20).nullable()
    val startDate = date("start_date")
    val archivedAt = timestamp("archived_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object CheckIns : Table("check_ins") {
    val id = long("id").autoIncrement()
    val habitId = long("habit_id").references(Habits.id, onDelete = ReferenceOption.CASCADE)
    val day = date("day")
    val value = integer("value")
    val note = varchar("note", 500).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("check_ins_habit_day_key", habitId, day)
    }
}
