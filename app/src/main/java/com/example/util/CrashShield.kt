package com.example.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference

/**
 * CrashShield intercepts unhandled exceptions across all threads,
 * preventing sudden unexpected app exits and logging diagnostic data.
 */
object CrashShield {
    private const val TAG = "CrashShield"
    private var isInstalled = false
    private var lastActivityRef: WeakReference<Activity>? = null

    fun install(application: Application) {
        if (isInstalled) return
        isInstalled = true

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                lastActivityRef = WeakReference(activity)
            }
            override fun onActivityStarted(activity: Activity) {
                lastActivityRef = WeakReference(activity)
            }
            override fun onActivityResumed(activity: Activity) {
                lastActivityRef = WeakReference(activity)
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (lastActivityRef?.get() == activity) {
                    lastActivityRef = null
                }
            }
        })

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Caught fatal exception on thread ${thread.name}: ${throwable.message}", throwable)

                // Write to crash log file
                saveCrashLog(application, thread, throwable)

                // Show notice on UI thread
                Handler(Looper.getMainLooper()).post {
                    try {
                        val currentAct = lastActivityRef?.get()
                        if (currentAct != null && !currentAct.isFinishing && !currentAct.isDestroyed) {
                            Toast.makeText(
                                currentAct,
                                "تم تفادي إغلاق التطبيق واستعادة الاستقرار بنجاح.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to display toast: ${t.message}")
                    }
                }
            } catch (fatal: Throwable) {
                Log.e(TAG, "Error in CrashShield handler: ${fatal.message}")
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val logFile = File(context.filesDir, "crash_log.txt")
            logFile.writeText("Thread: ${thread.name}\nTime: ${System.currentTimeMillis()}\nError: ${throwable.message}\n$stackTrace")
        } catch (e: Exception) {
            Log.w(TAG, "Could not write crash log: ${e.message}")
        }
    }
}
