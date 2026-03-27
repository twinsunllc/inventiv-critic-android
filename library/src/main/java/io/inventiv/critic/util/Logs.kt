package io.inventiv.critic.util

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object Logs {

    fun readLogcat(context: Context): File? {
        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "--pid=${android.os.Process.myPid()}", "-t", "500", "-v", "threadtime")
        )
        val file = File.createTempFile("logcat", ".txt", context.externalCacheDir)

        file.outputStream().buffered().use { out ->
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    out.write(line.toByteArray())
                    out.write("\n".toByteArray())
                }
            }
        }

        process.errorStream?.close()
        process.waitFor()

        return file
    }
}
