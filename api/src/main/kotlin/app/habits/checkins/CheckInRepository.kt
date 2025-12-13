package app.habits.checkins

import app.habits.common.DateRange
import app.habits.db.CheckIns
import app.habits.db.query
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate

class CheckInRepository(
    private val db: Database,
) {
    suspend fun upsert(
        habitId: Long,
        day: LocalDate,
        value: Int,
        note: String?,
        now: Instant,
    ): CheckIn =
        db.query {
            val updated =
                CheckIns.update({ (CheckIns.habitId eq habitId) and (CheckIns.day eq day) }) {
                    it[CheckIns.value] = value
                    it[CheckIns.note] = note
                    it[updatedAt] = now
                }
            if (updated == 0) {
                CheckIns.insert {
                    it[CheckIns.habitId] = habitId
                    it[CheckIns.day] = day
                    it[CheckIns.value] = value
                    it[CheckIns.note] = note
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            CheckIn(habitId, day, value, note, now)
        }

    suspend fun delete(
        habitId: Long,
        day: LocalDate,
    ): Boolean =
        db.query {
            CheckIns.deleteWhere { (CheckIns.habitId eq habitId) and (CheckIns.day eq day) } > 0
        }

    suspend fun find(
        habitId: Long,
        day: LocalDate,
    ): CheckIn? =
        db.query {
            CheckIns
                .selectAll()
                .where { (CheckIns.habitId eq habitId) and (CheckIns.day eq day) }
                .singleOrNull()
                ?.toCheckIn()
        }

    /** Check-ins of one habit in [range], newest first. */
    suspend fun list(
        habitId: Long,
        range: DateRange,
    ): List<CheckIn> =
        db.query {
            CheckIns
                .selectAll()
                .where { (CheckIns.habitId eq habitId) and (CheckIns.day.between(range.from, range.to)) }
                .orderBy(CheckIns.day to SortOrder.DESC)
                .map { it.toCheckIn() }
        }

    /** Every check-in of one habit, oldest first. */
    suspend fun all(habitId: Long): List<CheckIn> =
        db.query {
            CheckIns
                .selectAll()
                .where { CheckIns.habitId eq habitId }
                .orderBy(CheckIns.day to SortOrder.ASC)
                .map { it.toCheckIn() }
        }
}

private fun ResultRow.toCheckIn() =
    CheckIn(
        habitId = this[CheckIns.habitId],
        day = this[CheckIns.day],
        value = this[CheckIns.value],
        note = this[CheckIns.note],
        updatedAt = this[CheckIns.updatedAt],
    )
