package app.habits.habits

import app.habits.common.NotFoundException
import app.habits.common.today
import app.habits.users.User
import java.time.Clock

class HabitService(
    private val habits: HabitRepository,
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
}
