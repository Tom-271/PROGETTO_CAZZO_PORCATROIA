package com.example.progetto_tosa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.progetto_tosa.R

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        createNotificationChannel()

        val title = inputData.getString("title") ?: "Titolo"
        val message = inputData.getString("message") ?: "Messaggio"
        val notificationId = inputData.getInt("id", 1)

        sendNotification(notificationId, title, message)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Promemoria Allenamento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche giornaliere di allenamento"
            }

            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(id, builder.build())
        }
    }

    companion object {
        const val CHANNEL_ID = "WorkoutChannel"
    }
}