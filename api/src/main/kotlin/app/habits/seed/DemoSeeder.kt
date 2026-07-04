package app.habits.seed

import app.habits.auth.PasswordHasher
import app.habits.checkins.CheckIn
import app.habits.checkins.CheckInRepository
import app.habits.habits.HabitInput
import app.habits.habits.HabitRepository
import app.habits.habits.Schedule
import app.habits.stats.startOfWeek
import app.habits.users.UserRepository
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

const val DEMO_EMAIL = "demo@example.com"
const val DEMO_PASSWORD = "demo-password"

/**
 * Creates the demo account with about half a year of habits and check-ins, ending today. The data is
 * random but seeded, so every fresh database looks the same relative to the day it was created.
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
        if (users.findByEmail(DEMO_EMAIL) != null) {
            log.info("Demo user already exists, skipping seed")
            return
        }
        val zone = ZoneId.of("Europe/Berlin")
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val user = users.create(DEMO_EMAIL, hasher.hash(DEMO_PASSWORD), "Alex Demo", zone, now)
        val random = Random(42)
        // A two week holiday where most habits slipped.
        val holiday = today.minusDays(75)..today.minusDays(62)
        val entries = mutableListOf<CheckIn>()

        fun habit(
            name: String,
            color: String,
            schedule: Schedule,
            daysAgo: Long,
            target: Int? = null,
            unit: String? = null,
            description: String? = null,
        ) = HabitInput(name, description, color, schedule, target, unit, today.minusDays(daysAgo))

        suspend fun add(
            input: HabitInput,
            until: LocalDate = today,
            value: (LocalDate) -> Pair<Int, String?>?,
        ): Long {
            val created = habits.create(user.id, input, now)
            var day = input.startDate
            while (!day.isAfter(until)) {
                value(day)?.let { (v, note) -> entries += CheckIn(created.id, day, v, note, now) }
                day = day.plusDays(1)
            }
            return created.id
        }

        fun chance(
            day: LocalDate,
            normal: Double,
        ) = random.nextDouble() < if (day in holiday) normal / 3 else normal

        add(
            habit("Drink water", "#3fa7d6", Schedule.Daily, 190, target = 8, unit = "glasses"),
        ) { day ->
            when {
                day == today -> 5 to null
                day > today.minusDays(10) -> random.nextInt(8, 11) to null
                chance(day, 0.9) ->
                    (if (random.nextDouble() < 0.78) random.nextInt(8, 11) else random.nextInt(3, 8)) to
                        null
                else -> null
            }
        }

        val books = listOf("Atomic Habits", "The Hobbit", "Project Hail Mary", "Thinking, Fast and Slow")
        add(
            habit(
                "Read",
                "#8e6cef",
                Schedule.Daily,
                160,
                target = 20,
                unit = "minutes",
                description = "Before bed, no phone.",
            ),
        ) { day ->
            when {
                day == today -> null
                chance(day, 0.72) -> {
                    val minutes = if (random.nextDouble() < 0.85) random.nextInt(20, 50) else random.nextInt(5, 20)
                    val note =
                        if (random.nextDouble() <
                            0.12
                        ) {
                            books[(day.toEpochDay() / 40 % books.size).toInt()]
                        } else {
                            null
                        }
                    minutes to note
                }
                else -> null
            }
        }

        val runNotes = listOf("Easy pace", "Intervals", "Hilly route", "Felt heavy today", "New personal best!")
        add(
            habit(
                "Morning run",
                "#ef6c57",
                Schedule.Weekdays(setOf(MONDAY, WEDNESDAY, FRIDAY)),
                150,
                target = 5,
                unit = "km",
            ),
        ) { day ->
            val scheduled = day.dayOfWeek in setOf(MONDAY, WEDNESDAY, FRIDAY)
            when {
                day == today -> null
                scheduled && day > today.minusDays(15) -> random.nextInt(5, 9) to null
                scheduled && chance(day, 0.8) -> {
                    val km = if (random.nextDouble() < 0.85) random.nextInt(5, 11) else random.nextInt(2, 5)
                    km to if (random.nextDouble() < 0.15) runNotes[random.nextInt(runNotes.size)] else null
                }
                day.dayOfWeek == DayOfWeek.SATURDAY && random.nextDouble() < 0.2 -> random.nextInt(8, 16) to "Long run"
                else -> null
            }
        }

        add(habit("Meditate", "#2bb673", Schedule.Daily, 120, description = "Ten minutes with the app.")) { day ->
            when {
                day > today.minusDays(13) -> 1 to null
                chance(day, 0.62) -> 1 to if (random.nextDouble() < 0.05) "Hard to focus" else null
                else -> null
            }
        }

        add(habit("Strength training", "#f2a541", Schedule.TimesPerWeek(3), 140)) { day ->
            val inLastWeeks = day >= today.startOfWeek(MONDAY).minusWeeks(4)
            val gymDay = day.dayOfWeek in setOf(TUESDAY, THURSDAY, DayOfWeek.SATURDAY)
            when {
                day == today -> null
                gymDay && inLastWeeks -> 1 to null
                gymDay && chance(day, 0.75) -> 1 to if (random.nextDouble() < 0.1) "Leg day" else null
                !gymDay && random.nextDouble() < 0.06 -> 1 to null
                else -> null
            }
        }

        val prompts = listOf("Grateful for a quiet morning", "Busy day at work", "Called mum", "Tired but ok")
        add(habit("Journal", "#d4598e", Schedule.Daily, 100)) { day ->
            when {
                day == today -> null
                chance(day, 0.55) ->
                    1 to
                        if (random.nextDouble() < 0.25) prompts[random.nextInt(prompts.size)] else null
                else -> null
            }
        }

        add(
            habit(
                "No screens after 22:00",
                "#5c6bc0",
                Schedule.Weekdays(setOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY)),
                60,
                description = "School nights only.",
            ),
        ) { day ->
            if (day != today && chance(day, 0.6)) 1 to null else null
        }

        val spanishUntil = today.minusDays(30)
        val spanish =
            add(
                habit("Practice Spanish", "#26a69a", Schedule.Daily, 200, target = 15, unit = "minutes"),
                until = spanishUntil,
            ) { day -> if (chance(day, 0.5)) random.nextInt(10, 30) to null else null }
        habits.setArchived(user.id, spanish, spanishUntil.atStartOfDay(zone).toInstant(), now)

        checkIns.insertAll(entries, now)
        log.info("Seeded demo user {} with {} check-ins", DEMO_EMAIL, entries.size)
    }
}
