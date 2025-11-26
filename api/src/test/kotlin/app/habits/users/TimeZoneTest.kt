package app.habits.users

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeZoneTest {
    @Test
    fun `accepts region ids and utc`() {
        assertEquals(ZoneId.of("Europe/Berlin"), parseTimeZone("Europe/Berlin"))
        assertEquals(ZoneId.of("America/New_York"), parseTimeZone(" America/New_York "))
        assertEquals(ZoneId.of("UTC"), parseTimeZone("UTC"))
    }

    @Test
    fun `rejects offsets and unknown names`() {
        assertNull(parseTimeZone("+02:00"))
        assertNull(parseTimeZone("Mars/Olympus_Mons"))
        assertNull(parseTimeZone(""))
    }
}
