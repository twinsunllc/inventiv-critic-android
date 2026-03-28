package io.inventiv.critic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CriticTest {

    private val sampleMeminfo = """
        MemTotal:       16384000 kB
        MemFree:         2048000 kB
        MemAvailable:    4096000 kB
        Buffers:          512000 kB
        Cached:          3072000 kB
        SwapCached:            0 kB
        Active:          5120000 kB
        Inactive:        2560000 kB
        Active(anon):    3072000 kB
        Inactive(anon):   512000 kB
        Active(file):    2048000 kB
        Inactive(file):  2048000 kB
    """.trimIndent()

    @Test
    fun `parseProcMeminfoValue returns bytes for Active key`() {
        val result = Critic.parseProcMeminfoValue(sampleMeminfo, "Active")
        // 5120000 kB * 1024 = 5242880000 bytes
        assertEquals(5242880000L, result)
    }

    @Test
    fun `parseProcMeminfoValue returns bytes for MemTotal key`() {
        val result = Critic.parseProcMeminfoValue(sampleMeminfo, "MemTotal")
        // 16384000 kB * 1024 = 16777216000 bytes
        assertEquals(16777216000L, result)
    }

    @Test
    fun `parseProcMeminfoValue returns null for unknown key`() {
        val result = Critic.parseProcMeminfoValue(sampleMeminfo, "NonExistentKey")
        assertNull(result)
    }

    @Test
    fun `parseProcMeminfoValue does not match partial key names`() {
        // Content has only "Active(anon):" — no "Active:" line
        // Searching for "Active" must return null because "Active(anon)" is not a prefix match for "Active:"
        val content = "Active(anon): 3072000 kB\n"
        val result = Critic.parseProcMeminfoValue(content, "Active")
        assertNull(result)
    }

    @Test
    fun `parseProcMeminfoValue handles empty content`() {
        val result = Critic.parseProcMeminfoValue("", "Active")
        assertNull(result)
    }

    @Test
    fun `parseProcMeminfoValue handles content without matching key`() {
        val content = "MemTotal: 1024 kB\nMemFree: 512 kB\n"
        val result = Critic.parseProcMeminfoValue(content, "Active")
        assertNull(result)
    }

    @Test
    fun `parseProcMeminfoValue converts kB to bytes`() {
        val content = "Active: 1024 kB\n"
        val result = Critic.parseProcMeminfoValue(content, "Active")
        assertEquals(1048576L, result) // 1024 kB * 1024 = 1048576 bytes
    }
}
