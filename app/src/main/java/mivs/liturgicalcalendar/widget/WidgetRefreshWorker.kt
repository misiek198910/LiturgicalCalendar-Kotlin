package mivs.liturgicalcalendar.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val WIDGET_REFRESH_WORK_NAME = "WidgetMidnightRefresh"

/**
 * Odświeża wszystkie instancje [CalendarWidgetFree] i [CalendarWidgetPremium], żeby po
 * północy pokazywały nowy dzień liturgiczny bez czekania na ręczne otwarcie aplikacji.
 * `updateAll` jest tanim no-opem dla typu widgetu, który nie ma aktualnie żadnej instancji.
 */
class WidgetRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        CalendarWidgetFree().updateAll(applicationContext)
        CalendarWidgetPremium().updateAll(applicationContext)
        return Result.success()
    }
}

/** Uruchamiane z `onEnabled` obu receiverów przy dodaniu pierwszego widgetu (dowolnego typu). */
fun scheduleWidgetMidnightRefresh(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(millisUntilNextMidnight(), TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WIDGET_REFRESH_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}

/** Uruchamiane z `onDisabled` obu receiverów, gdy [anyCalendarWidgetActive] zwróci false. */
fun cancelWidgetMidnightRefresh(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WIDGET_REFRESH_WORK_NAME)
}

/**
 * Sprawdza, czy na pulpicie zostały jeszcze jakieś instancje Free lub Premium widgetu —
 * potrzebne, bo usunięcie jednego typu widgetu nie może wyłączyć odświeżania drugiego.
 */
internal suspend fun anyCalendarWidgetActive(context: Context): Boolean {
    val manager = GlanceAppWidgetManager(context)
    return manager.getGlanceIds(CalendarWidgetFree::class.java).isNotEmpty() ||
        manager.getGlanceIds(CalendarWidgetPremium::class.java).isNotEmpty()
}

private fun millisUntilNextMidnight(): Long {
    val now = Calendar.getInstance()
    val nextMidnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return nextMidnight.timeInMillis - now.timeInMillis
}
