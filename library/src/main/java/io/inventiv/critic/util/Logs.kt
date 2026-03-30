package io.inventiv.critic.util

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Calendar

object Logs {

    private val THREADTIME_REGEX = Regex(
        """^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEF])\s+(.+)$"""
    )

    private val LEVEL_MAP = mapOf(
        'V' to "VERBOSE",
        'D' to "DEBUG",
        'I' to "INFO",
        'W' to "WARN",
        'E' to "ERROR",
        'F' to "FATAL",
    )

    internal fun normalizeLogLine(line: String, year: Int): String {
        val match = THREADTIME_REGEX.matchEntire(line) ?: return line
        val (monthDay, time, levelChar, tagAndMessage) = match.destructured
        val level = LEVEL_MAP[levelChar.first()] ?: levelChar
        val isoTimestamp = "${year}-${monthDay}T${time}Z"
        return "[$isoTimestamp] $level: $tagAndMessage"
    }

    fun readLogcat(context: Context): File? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "--pid=${android.os.Process.myPid()}", "-t", "500", "-v", "threadtime")
        )
        val cacheDir = context.externalCacheDir ?: return null
        val file = File(cacheDir, "console-logs.txt")

        file.outputStream().buffered().use { out ->
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    val normalized = normalizeLogLine(line, currentYear)
                    out.write(normalized.toByteArray())
                    out.write("\n".toByteArray())
                }
            }
        }

        process.errorStream?.close()
        process.waitFor()

        return file
    }
}
