package mivs.liturgicalcalendar.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Łapie ręczną zmianę zegara/strefy czasowej (np. podróż), żeby oba widgety
 * ([CalendarWidgetFree], [CalendarWidgetPremium]) pokazały właściwy dzień liturgiczny
 * od razu, a nie dopiero po kolejnym cyklu [WidgetRefreshWorker]. Zmiana samej daty
 * o północy jest już pokrywana przez ten worker, bo ACTION_DATE_CHANGED nie jest
 * zwolniony z ograniczeń niejawnych broadcastów na API 26+ i nie zadziałałby
 * zadeklarowany w manifeście.
 */
class WidgetDateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CalendarWidgetFree().updateAll(context)
                CalendarWidgetPremium().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
