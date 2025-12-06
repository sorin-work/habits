package app.habits.habits

import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.checkins.CheckInRequest
import app.habits.checkins.MAX_CHECK_IN_VALUE
import app.habits.checkins.MAX_NOTE_LENGTH
import app.habits.common.ApiException
import app.habits.common.NotFoundException
import app.habits.common.today
import app.habits.common.validate
import app.habits.users.User
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.LocalDate

class HabitService(
    private val habits: HabitRepository,
    private val checkIns: CheckInRepository,
    private val clock: Clock,
) {
    suspend fun list(user: User): List<Habit> = habits.list(user.id)

    suspend fun get(
        user: User,
        id: Long,
    ): Habit = habits.find(user.id, id) ?: throw NotFoundException("Habit")

    suspend fun create(
        user: User,
        request: HabitRequest,
    ): Habit {
        val input = request.validated(clock.today(user.timeZone))
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

    suspend fun checkIn(
        user: User,
        id: Long,
        date: LocalDate,
        request: CheckInRequest,
    ): Pair<Habit, CheckIn> {
        val habit = get(user, id)
        checkDate(user, habit, date)
        val note = request.note?.trim()?.ifEmpty { null }
        val value = request.value ?: 1
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
        if (!checkIns.delete(habit.id, date)) throw NotFoundException("Check-in")
    }

    private fun checkDate(
        user: User,
        habit: Habit,
        date: LocalDate,
    ) {
        if (date.isAfter(LocalDate.now(clock))) {
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
}
