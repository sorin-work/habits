package app.habits.seed

import app.habits.auth.PasswordHasher
import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.habits.HabitInput
import app.habits.habits.HabitRepository
import app.habits.habits.Schedule
import app.habits.users.UserRepository
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

const val DEMO_EMAIL = "demo@example.com"
const val DEMO_PASSWORD = "demo-password"

/**
 * Creates the demo account with a few months of habits and check-ins, ending today. The data is random
 * but seeded, so every fresh database looks the same relative to the day it was created.
 */
class DemoSeeder(
    private val users: UserRepository,
    private val habits: HabitRepository,
    private val checkIns: CheckInRepository,
    private val hasher: PasswordHasher,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(DemoSeeder::class.java)

    suspend fun seed() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val user = users.create(DEMO_EMAIL, hasher.hash(DEMO_PASSWORD), "Alex Demo", zone, now)
        val random = Random(42)
        val entries = mutableListOf<CheckIn>()

        suspend fun add(
            name: String,
            color: String,
            schedule: Schedule,
            daysAgo: Long,
            target: Int? = null,
            unit: String? = null,
            until: LocalDate = today,
            value: (LocalDate) -> Int?,
        ): Long {
            val start = today.minusDays(daysAgo)
            val created = habits.create(user.id, HabitInput(name, null, color, schedule, target, unit, start), now)
            var day = start
            while (!day.isAfter(until)) {
                value(day)?.let { entries += CheckIn(created.id, day, it, null, now) }
                day = day.plusDays(1)
            }
            return created.id
        }

        add("Drink water", "#3fa7d6", Schedule.Daily, 120, target = 8, unit = "glasses") { day ->
            when {
                day == today -> 5
                random.nextDouble() < 0.85 -> random.nextInt(5, 11)
                else -> null
            }
        }
        add("Read", "#8e6cef", Schedule.Daily, 100, target = 20, unit = "minutes") { day ->
            if (day != today && random.nextDouble() < 0.7) random.nextInt(10, 45) else null
        }
        add(
            "Morning run",
            "#ef6c57",
            Schedule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY)),
            90,
            target = 5,
            unit = "km",
        ) { day ->
            val scheduled = day.dayOfWeek in setOf(MONDAY, WEDNESDAY, FRIDAY)
            if (day != today && scheduled && random.nextDouble() < 0.8) random.nextInt(4, 10) else null
        }
        add("Meditate", "#2bb673", Schedule.Daily, 80) { day ->
            if (day > today.minusDays(9) || random.nextDouble() < 0.6) 1 else null
        }
        add("Strength training", "#f2a541", Schedule.TimesPerWeek(3), 70) { day ->
            val gymDay = day.dayOfWeek in setOf(TUESDAY, THURSDAY, SATURDAY)
            if (day != today && gymDay && random.nextDouble() < 0.8) 1 else null
        }
        add("Journal", "#d4598e", Schedule.Daily, 60) { day ->
            if (day != today && random.nextDouble() < 0.55) 1 else null
        }

        checkIns.insertAll(entries, now)
        log.info("Seeded demo user {} with {} check-ins", DEMO_EMAIL, entries.size)
    }
}
