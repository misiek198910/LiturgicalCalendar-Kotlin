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
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.MainActivity
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.SubscriptionActivity
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Widget Premium (4x2): kolor liturgiczny na całą wysokość, pełna nazwa dnia/święta,
 * sigla i refren psalmu, wyróżniony cytat z Ewangelii i status wstrzemięźliwości
 * (patrz project_change.md pkt 3-4, kolumna Premium). Patron dnia i liturgia godzin
 * celowo pominięte — nazwa dnia (np. "Św. Mateusza Apostoła i Ewangelisty") już to
 * pokrywa, a liturgii godzin nie ma w bazie.
 *
 * Nawigacja strzałkami wczoraj/jutra została wycofana — na Glance każde kliknięcie
 * wymaga odbudowania "sesji" widgetu (kilka sekund opóźnienia zanim treść faktycznie
 * się zmieni), co sprawiało wrażenie zepsutej funkcji. Zamiast tego widget zawsze
 * pokazuje dzisiejsze czytania, bez elementów interaktywnych poza otwarciem aplikacji.
 *
 * Widget jest dostępny w pickerze dla każdego użytkownika — status subskrypcji sprawdzamy
 * dopiero przy renderowaniu i bez aktywnej subskrypcji pokazujemy zachętę do wykupienia
 * zamiast pełnej treści.
 */
class CalendarWidgetPremium : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPremium = isPremiumUser(context)
        val repository = CalendarRepository(context)
        val day = repository.getDay(LocalDate.now())

        val psalmLine: String?
        val gospelExcerpt: String?
        if (isPremium) {
            val readings = repository.getReadingsForDay(day)
            // Bez agresywnego ucinania — widget może być powiększony przez użytkownika
            // (resizeMode w metadanych), a Android sam przełamie/obetnie tekst (maxLines
            // + ellipsis) do faktycznie dostępnej wysokości. Cap tylko jako zabezpieczenie
            // przed nietypowo długim wpisem w bazie.
            psalmLine = readings.psalmResponse.take(400)
            gospelExcerpt = readings.gospelFullText?.take(2000)
        } else {
            psalmLine = null
            gospelExcerpt = null
        }

        provideContent {
            CalendarWidgetPremiumContent(day, isPremium, psalmLine, gospelExcerpt)
        }
    }
}

@Composable
private fun CalendarWidgetPremiumContent(
    day: LiturgicalDay,
    isPremium: Boolean,
    psalmLine: String?,
    gospelExcerpt: String?
) {
    val liturgicalColor = liturgicalColorProvider(day.colorCode)
    val locale = Locale.forLanguageTag("pl-PL")

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.button_dark_grey))
    ) {
        Spacer(modifier = GlanceModifier.width(6.dp).fillMaxHeight().background(liturgicalColor))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .clickable(
                    if (isPremium) actionStartActivity<MainActivity>()
                    else actionStartActivity<SubscriptionActivity>()
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${day.date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }}, " +
                    "${day.date.dayOfMonth} ${day.date.month.getDisplayName(JavaTextStyle.FULL, locale)}",
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(R.color.white),
                    fontSize = 16.sp(),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = day.feastName ?: "Feria",
                maxLines = 1,
                style = TextStyle(color = ColorProvider(R.color.white), fontSize = 13.sp())
            )
            Spacer(modifier = GlanceModifier.height(6.dp))

            if (isPremium) {
                Text(
                    text = psalmLine ?: "Brak danych o psalmie na dziś",
                    maxLines = 3,
                    style = TextStyle(color = ColorProvider(R.color.white), fontSize = 11.sp())
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = gospelExcerpt?.let { "„$it”" } ?: "Brak treści Ewangelii na dziś",
                    maxLines = 12,
                    style = TextStyle(
                        color = ColorProvider(R.color.white),
                        fontSize = 11.sp(),
                        fontStyle = FontStyle.Italic
                    )
                )
                if (isAbstinenceDay(day)) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Wstrzemięźliwość od pokarmów mięsnych",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(R.color.liturgical_gold),
                            fontSize = 11.sp(),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Text(
                    text = "🔒 Odblokuj psalm, cytat Ewangelii i status postu w Premium",
                    maxLines = 2,
                    style = TextStyle(color = ColorProvider(R.color.gray_non_premium), fontSize = 12.sp())
                )
            }
        }
    }
}

class CalendarWidgetPremiumReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidgetPremium()

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
