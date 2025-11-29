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
}
