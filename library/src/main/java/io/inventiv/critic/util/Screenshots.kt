package io.inventiv.critic.util

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Screenshots {

    suspend fun captureActivity(activity: Activity): File {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captureView(activity)
        } else {
            captureViewLegacy(activity)
        }
        return store(bitmap, activity.cacheDir, "last_screen.png")
    }

    private fun captureViewLegacy(activity: Activity): Bitmap {
        val view = activity.window.decorView
        view.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(view.drawingCache)
        view.isDrawingCacheEnabled = false
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun captureView(activity: Activity): Bitmap {
        val window = activity.window
        val view = window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        return suspendCoroutine { continuation ->
            PixelCopy.request(
                window,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        continuation.resume(bitmap)
                    } else {
                        continuation.resumeWithException(
                            RuntimeException("PixelCopy failed with result: $result")
                        )
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    fun store(bitmap: Bitmap, directory: File, fileName: String): File {
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }
}
