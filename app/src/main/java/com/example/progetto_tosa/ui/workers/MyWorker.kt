package com.example.progetto_tosa.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.navigation.NavDeepLinkBuilder
import androidx.core.os.bundleOf
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.progetto_tosa.R
import com.example.progetto_tosa.ui.account.MainActivity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Worker che mostra la notifica "Hai aggiornato il tuo peso?" */
class MyWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // (Opzionale) salta se già loggato oggi
        if (alreadyLoggedToday()) return Result.success()

        NotificationUtils.createNotificationChannel(applicationContext)

        val title = inputData.getString(KEY_TITLE) ?: DEFAULT_TITLE
        val message = inputData.getString(KEY_MESSAGE) ?: DEFAULT_MESSAGE
        val id = inputData.getInt(KEY_ID, DEFAULT_ID)

        sendNotification(id, title, message)
        return Result.success()
    }

    private fun alreadyLoggedToday(): Boolean {
        val prefs = applicationContext.getSharedPreferences("weights", Context.MODE_PRIVATE)
        val last = prefs.getLong("last_weight_ts", 0L)
        if (last == 0L) return false

        val lastDate = java.time.Instant.ofEpochMilli(last)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return lastDate == LocalDate.now()
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val ctx = applicationContext
        if (!NotificationUtils.canPostNotifications(ctx)) return

        val pendingIntent = NavDeepLinkBuilder(ctx)
            .setComponentName(com.example.progetto_tosa.ui.account.MainActivity::class.java)
            .setGraph(R.navigation.mobile_navigation)          // <-- usa il TUO file nav (es. mobile_navigation.xml)
            .setDestination(R.id.progressionFragment)         // <-- ID reale del fragment nel graph
            // .setArguments(bundleOf("key" to "value"))        // se ti servono argomenti
            .createPendingIntent()

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build())
        } catch (se: SecurityException) {
            // logga se vuoi
        }
    }

    companion object {
        const val CHANNEL_ID = "WorkoutChannel"

        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_ID = "id"

        const val DEFAULT_TITLE = "Hai aggiornato il tuo peso?"
        const val DEFAULT_MESSAGE = "Apri il profilo e inserisci il tuo peso corporeo e bf di oggi."
        const val DEFAULT_ID = 1001
    }
}

/** Scheduler per il Worker */
object WeightReminderScheduler {

    private const val UNIQUE_NAME = "weight_reminder"

    fun scheduleDaily(context: Context, hour: Int = 20, minute: Int = 0) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (next.isBefore(now)) next = next.plusDays(1)
        val delayMs = Duration.between(now, next).toMillis()

        val data = Data.Builder()
            .putString(MyWorker.KEY_TITLE, MyWorker.DEFAULT_TITLE)
            .putString(MyWorker.KEY_MESSAGE, MyWorker.DEFAULT_MESSAGE)
            .putInt(MyWorker.KEY_ID, MyWorker.DEFAULT_ID)
            .build()

        val request = PeriodicWorkRequestBuilder<MyWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}

/** Utilità notifiche + permessi */
object NotificationUtils {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MyWorker.CHANNEL_ID,
                "Promemoria Allenamento/Peso",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifiche giornaliere (peso, allenamento, ecc.)" }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
}
