package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.TrackerDatabase
import com.example.data.TrackerLog
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.io.StringWriter

class TrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Setup uncaught exception handler to prevent silent deaths and notify user
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleCrash(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        val shortMessage = throwable.localizedMessage ?: "Unknown application crash"
        
        Log.e("TrackerApplication", "CRITICAL: Application crashed! $shortMessage\n$stackTrace")
        
        // 1. Log crash details directly into local Room Database logs synchronously
        try {
            val db = TrackerDatabase.getDatabase(this)
            val dao = db.trackerDao()
            runBlocking {
                dao.insertLog(
                    TrackerLog(
                        type = "CRASH",
                        message = "CRASH: $shortMessage\n${stackTrace.take(1500)}",
                        status = "ERROR"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("TrackerApplication", "Failed to log crash to database: ${e.localizedMessage}")
        }

        // 2. Show local high-priority notification alerting the user of the service failure
        try {
            showCrashNotification(shortMessage)
        } catch (e: Exception) {
            Log.e("TrackerApplication", "Failed to show crash notification: ${e.localizedMessage}")
        }
    }

    private fun showCrashNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "tracker_crash_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tracker Crashes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when the background location tracking system crashes"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            999,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Find My Device Tracker Stopped")
            .setContentText("Background tracker halted: $message. Tap to relaunch immediately.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Background location tracking has been halted due to a crash: $message.\n\nTap this alert immediately to relaunch Find My Device and restore real-time tracking protection."))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(999, notification)
    }
}
