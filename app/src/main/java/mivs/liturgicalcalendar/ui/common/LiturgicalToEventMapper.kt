package mivs.liturgicalcalendar.ui.common

import com.applandeo.materialcalendarview.EventDay
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.util.Calendar

object LiturgicalToEventMapper {

    fun map(day: LiturgicalDay): EventDay {
        // 1. Konwersja LocalDate (Java Time) -> Calendar (Stary format wymagany przez Applandeo)
        val calendar = Calendar.getInstance()
        calendar.set(day.date.year, day.date.monthValue - 1, day.date.dayOfMonth)

        // 2. Dobór ikony na podstawie okresu liturgicznego
        val iconRes = when (day.season) {
            LiturgicalSeason.ADVENT -> {
                // TODO: Tu można dodać logikę dla Niedzieli Gaudete (różowy)
                R.drawable.priest_violet
            }
            LiturgicalSeason.CHRISTMAS -> R.drawable.priest_white
            LiturgicalSeason.LENT -> {
                // TODO: Tu można dodać logikę dla Niedzieli Laetare (różowy)
                R.drawable.priest_violet
            }
            LiturgicalSeason.TRIDUUM -> R.drawable.priest_white // Lub czerwony dla Piątku
            LiturgicalSeason.EASTER -> R.drawable.priest_white
            LiturgicalSeason.PENTECOST -> R.drawable.priest_red
            LiturgicalSeason.ORDINARY_TIME -> R.drawable.priest_green
        }

        // 3. Zwracamy obiekt gotowy dla widoku
        return EventDay(calendar, iconRes)
    }
}