package app.habits.habits

import app.habits.db.Habits
import app.habits.db.query
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.time.Instant

class HabitRepository(
    private val db: Database,
) {
    suspend fun list(
        userId: Long,
        archived: Boolean? = null,
    ): List<Habit> =
        db.query {
            Habits
                .selectAll()
                .where {
                    val owner = Habits.userId eq userId
                    when (archived) {
                        null -> owner
                        true -> owner and Habits.archivedAt.isNotNull()
                        false -> owner and Habits.archivedAt.isNull()
                    }
                }.orderBy(Habits.position to SortOrder.ASC, Habits.id to SortOrder.ASC)
                .map { it.toHabit() }
        }

    suspend fun find(
        userId: Long,
        id: Long,
    ): Habit? =
        db.query {
            Habits
                .selectAll()
                .where { (Habits.id eq id) and (Habits.userId eq userId) }
                .singleOrNull()
                ?.toHabit()
        }

    suspend fun create(
        userId: Long,
        input: HabitInput,
        now: Instant,
    ): Habit =
        db.query {
            val position = nextPosition(userId)
            val id =
                Habits.insert {
                    it[Habits.userId] = userId
                    it.write(input)
                    it[Habits.position] = position
                    it[createdAt] = now
                    it[updatedAt] = now
                } get Habits.id
            Habits
                .selectAll()
                .where { Habits.id eq id }
                .single()
                .toHabit()
        }

    suspend fun update(
        userId: Long,
        id: Long,
        input: HabitInput,
        now: Instant,
    ): Boolean =
        db.query {
            Habits.update({ (Habits.id eq id) and (Habits.userId eq userId) }) {
                it.write(input)
                it[updatedAt] = now
            } > 0
        }

    suspend fun setArchived(
        userId: Long,
        id: Long,
        archivedAt: Instant?,
        now: Instant,
    ): Boolean =
        db.query {
            Habits.update({ (Habits.id eq id) and (Habits.userId eq userId) }) {
                it[Habits.archivedAt] = archivedAt
                it[updatedAt] = now
            } > 0
        }

    suspend fun reorder(
        userId: Long,
        ids: List<Long>,
        now: Instant,
    ) = db.query {
        ids.forEachIndexed { index, id ->
            Habits.update({ (Habits.id eq id) and (Habits.userId eq userId) }) {
                it[position] = index
                it[updatedAt] = now
            }
        }
    }

    suspend fun delete(
        userId: Long,
        id: Long,
    ): Boolean =
        db.query {
            Habits.deleteWhere { (Habits.id eq id) and (Habits.userId eq userId) } > 0
        }

    private fun nextPosition(userId: Long): Int {
        val max = Habits.position.max()
        val current =
            Habits
                .select(max)
                .where { Habits.userId eq userId }
                .single()[max]
        return (current ?: -1) + 1
    }
}

private fun UpdateBuilder<*>.write(input: HabitInput) {
    val columns = input.schedule.toColumns()
    this[Habits.name] = input.name
    this[Habits.description] = input.description
    this[Habits.color] = input.color
    this[Habits.scheduleType] = columns.type
    this[Habits.scheduleDays] = columns.days
    this[Habits.timesPerWeek] = columns.timesPerWeek
    this[Habits.target] = input.target
    this[Habits.unit] = input.unit
    this[Habits.startDate] = input.startDate
}

fun ResultRow.toHabit() =
    Habit(
        id = this[Habits.id],
        userId = this[Habits.userId],
        name = this[Habits.name],
        description = this[Habits.description],
        color = this[Habits.color],
        schedule = scheduleFromColumns(this[Habits.scheduleType], this[Habits.scheduleDays], this[Habits.timesPerWeek]),
        target = this[Habits.target],
        unit = this[Habits.unit],
        startDate = this[Habits.startDate],
        position = this[Habits.position],
        archivedAt = this[Habits.archivedAt],
        createdAt = this[Habits.createdAt],
    )
