package mivs.liturgicalcalendar.widget

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.glance.unit.ColorProvider
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.DayOfWeek

internal fun liturgicalColorProvider(colorCode: String?) = when (colorCode?.lowercase()) {
    "r" -> ColorProvider(R.color.liturgical_red)
    "w" -> ColorProvider(R.color.liturgical_white)
    "v" -> ColorProvider(R.color.liturgical_violet)
    "p" -> ColorProvider(R.color.liturgical_rose)
    "g" -> ColorProvider(R.color.liturgical_green)
    else -> ColorProvider(R.color.liturgical_green)
}

internal fun Int.sp(): TextUnit = TextUnit(this.toFloat(), TextUnitType.Sp)

/**
 * Status premium jest cache'owany lokalnie w Room przez [mivs.liturgicalcalendar.billing.BillingManager]
 * po każdej synchronizacji z Google Play. Czytamy stąd bezpośrednio zamiast z jego `LiveData`,
 * bo w procesie renderowania widgetu ta `LiveData` startuje zawsze od `false` i nie zdąży się
 * zaktualizować na czas — Room jest już źródłem prawdy używanym przez resztę aplikacji.
 */
internal suspend fun isPremiumUser(context: Context): Boolean {
    return AppDatabase.getDatabase(context).userStatusDao().getStatus()?.isPremium ?: false
}

/**
 * Kościelna reguła wstrzemięźliwości od pokarmów mięsnych: każdy piątek roku poza okresem
 * wielkanocnym i uroczystościami. W bazie feastów nie ma osobnego pola na to, więc liczymy
 * to z reguły zamiast fałszować dane.
 */
internal fun isAbstinenceDay(day: LiturgicalDay): Boolean {
    if (day.date.dayOfWeek != DayOfWeek.FRIDAY) return false
    if (day.season == LiturgicalSeason.EASTER) return false
    if (day.isSolemnity) return false
    return true
}
