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
import mivs.liturgicalcalendar.R 
import mivs.liturgicalcalendar.SettingsActivity 
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import java.time.LocalDate

class DailyFeastWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        
        val repository = CalendarRepository(applicationContext)
        val today = LocalDate.now()

        
        val dayInfo = LiturgicalCalendarCalc.generateDay(today)
        val readings = repository.getReadingsForDay(dayInfo)

        
        
        val title = readings.dbFeastName ?: dayInfo.feastName ?: "Liturgia dnia"

        
        val content = if (!readings.gospelFullText.isNullOrEmpty()) {
            "Ewangelia: " + readings.gospelFullText.take(90) + "..."
        } else {
            readings.psalmResponse
        }

        
        sendNotification(title, content)

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "daily_word_channel"

        
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel(channelId)

        
        val intent = Intent(applicationContext, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications) 
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) 
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