package app.habits.habits

import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.checkins.CheckInRequest
import app.habits.checkins.MAX_CHECK_IN_VALUE
import app.habits.checkins.MAX_NOTE_LENGTH
import app.habits.common.ApiException
import app.habits.common.ConflictException
import app.habits.common.DateRange
import app.habits.common.NotFoundException
import app.habits.common.today
import app.habits.common.validate
import app.habits.users.User
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.LocalDate

const val MAX_ACTIVE_HABITS = 50

class HabitService(
    private val habits: HabitRepository,
    private val checkIns: CheckInRepository,
    private val clock: Clock,
) {
    suspend fun list(
        user: User,
        archived: Boolean,
    ): List<Habit> = habits.list(user.id, archived)

    suspend fun get(
        user: User,
        id: Long,
    ): Habit = habits.find(user.id, id) ?: throw NotFoundException("Habit")

    suspend fun create(
        user: User,
        request: HabitRequest,
    ): Habit {
        val input = request.validated(clock.today(user.timeZone))
        if (habits.list(user.id, archived = false).size >= MAX_ACTIVE_HABITS) {
            throw ConflictException("habit_limit", "You can have at most $MAX_ACTIVE_HABITS active habits")
        }
        return habits.create(user.id, input, clock.instant())
    }

    suspend fun update(
        user: User,
        id: Long,
        request: HabitRequest,
    ): Habit {
        val existing = get(user, id)
        val input = request.validated(clock.today(user.timeZone), defaultStartDate = existing.startDate)
        habits.update(user.id, id, input, clock.instant())
        return get(user, id)
    }

    suspend fun delete(
        user: User,
        id: Long,
    ) {
        if (!habits.delete(user.id, id)) throw NotFoundException("Habit")
    }

    suspend fun archive(
        user: User,
        id: Long,
    ): Habit {
        val habit = get(user, id)
        if (!habit.archived) habits.setArchived(user.id, id, clock.instant(), clock.instant())
        return get(user, id)
    }

    suspend fun restore(
        user: User,
        id: Long,
    ): Habit {
        val habit = get(user, id)
        if (habit.archived) {
            if (habits.list(user.id, archived = false).size >= MAX_ACTIVE_HABITS) {
                throw ConflictException("habit_limit", "You can have at most $MAX_ACTIVE_HABITS active habits")
            }
            habits.setArchived(user.id, id, null, clock.instant())
        }
        return get(user, id)
    }

    suspend fun reorder(
        user: User,
        ids: List<Long>,
    ): List<Habit> {
        val active = habits.list(user.id, archived = false)
        validateReorder(ids, active.map { it.id }.toSet())
        habits.reorder(user.id, ids, clock.instant())
        return habits.list(user.id, archived = false)
    }

    suspend fun checkIn(
        user: User,
        id: Long,
        date: LocalDate,
        request: CheckInRequest,
    ): Pair<Habit, CheckIn> {
        val habit = get(user, id)
        checkDate(user, habit, date)
        val note = request.note?.trim()?.ifEmpty { null }
        val value = request.value ?: habit.dailyGoal
        validate {
            check("value", value in 1..MAX_CHECK_IN_VALUE) { "Value must be between 1 and $MAX_CHECK_IN_VALUE" }
            check("note", (note?.length ?: 0) <= MAX_NOTE_LENGTH) {
                "Note must be at most $MAX_NOTE_LENGTH characters"
            }
        }
        return habit to checkIns.upsert(habit.id, date, value, note, clock.instant())
    }

    suspend fun undoCheckIn(
        user: User,
        id: Long,
        date: LocalDate,
    ) {
        val habit = get(user, id)
        if (habit.archived) throw archived()
        if (!checkIns.delete(habit.id, date)) throw NotFoundException("Check-in")
    }

    suspend fun checkIns(
        user: User,
        id: Long,
        range: DateRange,
    ): Pair<Habit, List<CheckIn>> {
        val habit = get(user, id)
        return habit to checkIns.list(habit.id, range)
    }

    private fun checkDate(
        user: User,
        habit: Habit,
        date: LocalDate,
    ) {
        if (habit.archived) throw archived()
        if (date.isAfter(clock.today(user.timeZone))) {
            throw ApiException(
                HttpStatusCode.BadRequest,
                "future_date",
                "You can't check in for a day that hasn't happened yet",
            )
        }
        if (date.isBefore(habit.startDate)) {
            throw ApiException(HttpStatusCode.BadRequest, "before_start", "This habit starts on ${habit.startDate}")
        }
    }

    private fun archived() = ConflictException("habit_archived", "Restore this habit before changing its check-ins")
}
