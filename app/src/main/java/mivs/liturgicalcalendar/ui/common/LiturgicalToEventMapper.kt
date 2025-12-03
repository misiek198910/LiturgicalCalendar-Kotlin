package mivs.liturgicalcalendar.ui.common

import com.applandeo.materialcalendarview.EventDay
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.util.Calendar

object LiturgicalToEventMapper {

    fun map(day: LiturgicalDay): EventDay {
        val calendar = Calendar.getInstance()
        calendar.set(day.date.year, day.date.monthValue - 1, day.date.dayOfMonth)

        // LOGIKA WYBORU KOLORU
        // Priorytet 1: Kolor z bazy danych (jeśli istnieje)
        val iconRes = if (day.colorCode != null) {
            when (day.colorCode.lowercase()) {
                "r" -> R.drawable.priest_red    // Czerwony (Męczennicy)
                "w" -> R.drawable.priest_white  // Biały (Święci, Maryjne)
                "v" -> R.drawable.priest_violet // Fiolet (Adwent/Post)
                "p" -> R.drawable.priest_pink   // Różowy
                "g" -> R.drawable.priest_green  // Zielony
                else -> R.drawable.priest_green // Domyślny
            }
        } else {
            // Priorytet 2: Kolor z algorytmu (Okres liturgiczny)
            when (day.season) {
                LiturgicalSeason.ADVENT -> R.drawable.priest_violet
                LiturgicalSeason.CHRISTMAS -> R.drawable.priest_white
                LiturgicalSeason.LENT -> R.drawable.priest_violet
                LiturgicalSeason.TRIDUUM -> R.drawable.priest_red
                LiturgicalSeason.EASTER -> R.drawable.priest_white
                LiturgicalSeason.PENTECOST -> R.drawable.priest_red
                LiturgicalSeason.ORDINARY_TIME -> R.drawable.priest_green
            }
        }

        return EventDay(calendar, iconRes)
    }
}