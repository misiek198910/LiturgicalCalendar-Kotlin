package mivs.liturgicalcalendar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.MainActivity
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Widget Free (2x2): data, dzień tygodnia, skrócona nazwa dnia/święta i pasek koloru
 * liturgicznego — bez treści na pulpicie (patrz project_change.md pkt 3, kolumna Free).
 * Dane pochodzą z tego samego [CalendarRepository] (Room) co reszta aplikacji XML/Views.
 */
class CalendarWidgetFree : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = CalendarRepository(context)
        val day = repository.getDay(LocalDate.now())

        provideContent {
            CalendarWidgetFreeContent(day)
        }
    }
}

@Composable
private fun CalendarWidgetFreeContent(day: LiturgicalDay) {
    val liturgicalColor = liturgicalColorProvider(day.colorCode)
    val locale = Locale.forLanguageTag("pl-PL")

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.button_dark_grey))
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Spacer(modifier = GlanceModifier.width(6.dp).fillMaxHeight().background(liturgicalColor))

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day.date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale)
                    .replaceFirstChar { it.uppercase(locale) },
                style = TextStyle(color = ColorProvider(R.color.white), fontSize = 13.sp())
            )
            Text(
                text = "${day.date.dayOfMonth} ${
                    day.date.month.getDisplayName(JavaTextStyle.FULL, locale)
                }",
                style = TextStyle(
                    color = ColorProvider(R.color.white),
                    fontSize = 20.sp(),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = day.feastName ?: "Feria",
                maxLines = 2,
                style = TextStyle(color = ColorProvider(R.color.white), fontSize = 12.sp())
            )
        }
    }
}

class CalendarWidgetFreeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidgetFree()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!anyCalendarWidgetActive(context)) cancelWidgetMidnightRefresh(context)
            } finally {
                pendingResult.finish()
            }
        }
        super.onDisabled(context)
    }
}
