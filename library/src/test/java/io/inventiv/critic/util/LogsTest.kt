package io.inventiv.critic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LogsTest {

    @Test
    fun `normalizeLogLine converts INFO threadtime line`() {
        val line = "03-30 12:34:56.789 1234 1235 I MyTag: My message"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-03-30T12:34:56.789Z] INFO: MyTag: My message", result)
    }

    @Test
    fun `normalizeLogLine converts DEBUG threadtime line`() {
        val line = "01-15 08:00:00.001 100 101 D SomeTag: Debug output"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-01-15T08:00:00.001Z] DEBUG: SomeTag: Debug output", result)
    }

    @Test
    fun `normalizeLogLine converts WARN threadtime line`() {
        val line = "12-31 23:59:59.999 9 9 W WarnTag: Warning!"
        val result = Logs.normalizeLogLine(line, 2025)
        assertEquals("[2025-12-31T23:59:59.999Z] WARN: WarnTag: Warning!", result)
    }

    @Test
    fun `normalizeLogLine converts ERROR threadtime line`() {
        val line = "06-01 10:00:00.000 555 556 E ErrorTag: Something broke"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-06-01T10:00:00.000Z] ERROR: ErrorTag: Something broke", result)
    }

    @Test
    fun `normalizeLogLine converts VERBOSE threadtime line`() {
        val line = "03-30 09:00:00.123 200 201 V VerboseTag: verbose message"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-03-30T09:00:00.123Z] VERBOSE: VerboseTag: verbose message", result)
    }

    @Test
    fun `normalizeLogLine converts FATAL threadtime line`() {
        val line = "03-30 09:00:00.000 300 301 F FatalTag: fatal message"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-03-30T09:00:00.000Z] FATAL: FatalTag: fatal message", result)
    }

    @Test
    fun `normalizeLogLine passes through non-matching lines unchanged`() {
        val line = "--------- beginning of main"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("--------- beginning of main", result)
    }

    @Test
    fun `normalizeLogLine passes through empty line unchanged`() {
        val result = Logs.normalizeLogLine("", 2026)
        assertEquals("", result)
    }

    @Test
    fun `normalizeLogLine handles message with colon in tag`() {
        val line = "03-30 12:00:00.000 1 1 I Tag:Sub: message with colon"
        val result = Logs.normalizeLogLine(line, 2026)
        assertEquals("[2026-03-30T12:00:00.000Z] INFO: Tag:Sub: message with colon", result)
    }

    @Test
    fun `normalizeLogLine uses provided year`() {
        val line = "03-30 12:34:56.789 1234 1235 I MyTag: My message"
        assertEquals("[2024-03-30T12:34:56.789Z] INFO: MyTag: My message", Logs.normalizeLogLine(line, 2024))
        assertEquals("[2030-03-30T12:34:56.789Z] INFO: MyTag: My message", Logs.normalizeLogLine(line, 2030))
    }
}
