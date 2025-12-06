package mivs.liturgicalcalendar.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mivs.liturgicalcalendar.R // Upewnij się, że importujesz R ze swojego pakietu
import mivs.liturgicalcalendar.SettingsActivity // Do kliknięcia w powiadomienie (lub MainActivity)
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import java.time.LocalDate

class DailyFeastWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Inicjalizacja Repo
        val repository = CalendarRepository(applicationContext)
        val today = LocalDate.now()

        // 2. Pobranie danych o dniu
        val dayInfo = LiturgicalCalendarCalc.generateDay(today)
        val readings = repository.getReadingsForDay(dayInfo)

        // 3. Budowanie treści
        // Tytuł: Nazwa święta lub "Dzień powszedni"
        val title = readings.dbFeastName ?: dayInfo.feastName ?: "Liturgia dnia"

        // Treść: Ewangelia (skrót) lub Psalm
        val content = if (!readings.gospelFullText.isNullOrEmpty()) {
            "Ewangelia: " + readings.gospelFullText.take(90) + "..."
        } else {
            readings.psalmResponse
        }

        // 4. Wysłanie powiadomienia
        sendNotification(title, content)

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_word_channel"

        // Sprawdzenie uprawnień (dla Androida 13+)
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel(channelId)

        // Co się stanie po kliknięciu? Otwieramy SettingsActivity (lub zmień na MainActivity)
        val intent = Intent(applicationContext, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications) // Twoja biała ikona!
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Rozwija długi tekst
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1001, builder.build())
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Codzienne Słowo"
            val descriptionText = "Powiadomienia o patronie dnia i Ewangelii"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}