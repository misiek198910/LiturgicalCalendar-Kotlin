package mivs.liturgicalcalendar.domain.logic

import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

object LiturgicalCalendarCalc {

    fun generateDay(date: LocalDate): LiturgicalDay {
        val year = date.year
        val easter = EasterCalculator.calculate(year)

        // --- KOTWICE CZASOWE ---
        val ashWednesday = easter.minusDays(46)
        val pentecost = easter.plusWeeks(7)
        val corpusChristi = easter.plusDays(60)

        // Adwent
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val firstAdvent = calculateFirstAdventSunday(year)

        // --- 1. DETEKCJA OKRESU (SEASON) ---
        var season = LiturgicalSeason.ORDINARY_TIME

        // Boże Narodzenie (uproszczone ramy)
        val epiphany = LocalDate.of(year, Month.JANUARY, 6)
        val baptismOfLord = epiphany.plusDays((7 - epiphany.dayOfWeek.value % 7).toLong() + 1) // Niedziela po 6.01

        if ((date.month == Month.DECEMBER && date.dayOfMonth >= 25) ||
            (date.year == year && date.isBefore(baptismOfLord.plusDays(1)))) { // Do Chrztu Pańskiego
            season = LiturgicalSeason.CHRISTMAS
        }
        else if (!date.isBefore(ashWednesday) && date.isBefore(easter)) {
            season = LiturgicalSeason.LENT
        }
        else if (!date.isBefore(easter) && !date.isAfter(pentecost)) {
            season = LiturgicalSeason.EASTER
        }
        else if (!date.isBefore(firstAdvent) && date.isBefore(christmas)) {
            season = LiturgicalSeason.ADVENT
        }

        // --- 2. DETEKCJA NAZW ŚWIĄT I NIEDZIEL ---
        var feastName: String? = null
        var isSolemnity = false

        // A. Sztywne daty (Święta ruchome główne)
        if (date.isEqual(easter)) {
            feastName = "Niedziela Zmartwychwstania Pańskiego"
            isSolemnity = true
        } else if (date.isEqual(ashWednesday)) {
            feastName = "Środa Popielcowa"
        } else if (date.isEqual(corpusChristi)) {
            feastName = "Uroczystość Najświętszego Ciała i Krwi Chrystusa"
            isSolemnity = true
        } else if (date.isEqual(pentecost)) {
            feastName = "Niedziela Zesłania Ducha Świętego"
            isSolemnity = true
        } else if (date.month == Month.DECEMBER && date.dayOfMonth == 25) {
            feastName = "Uroczystość Narodzenia Pańskiego"
            isSolemnity = true
        } else if (date.month == Month.JANUARY && date.dayOfMonth == 6) {
            feastName = "Objawienie Pańskie (Trzech Króli)"
            isSolemnity = true
        }

        // B. Logika dla NIEDZIEL (jeśli nazwa jeszcze nie została ustalona)
        if (feastName == null && date.dayOfWeek == DayOfWeek.SUNDAY) {
            isSolemnity = true // Każda niedziela jest uroczystością

            when (season) {
                LiturgicalSeason.ADVENT -> {
                    // Liczymy tygodnie od 1. Niedzieli Adwentu
                    val weekNum = ChronoUnit.WEEKS.between(firstAdvent, date).toInt() + 1
                    feastName = "$weekNum. Niedziela Adwentu"
                }
                LiturgicalSeason.LENT -> {
                    // Liczymy tygodnie do Wielkanocy
                    val weeksBeforeEaster = ChronoUnit.WEEKS.between(date, easter).toInt()
                    if (weeksBeforeEaster == 1) {
                        feastName = "Niedziela Palmowa Męki Pańskiej"
                    } else {
                        // 1. niedziela postu jest 6 tyg przed Wielkanocą, 2. jest 5 tyg...
                        val lentSundayNum = 7 - weeksBeforeEaster
                        if (lentSundayNum in 1..5) {
                            feastName = "$lentSundayNum. Niedziela Wielkiego Postu"
                        }
                    }
                }
                LiturgicalSeason.EASTER -> {
                    // Liczymy tygodnie po Wielkanocy
                    val weeksAfter = ChronoUnit.WEEKS.between(easter, date).toInt()
                    val easterSundayNum = weeksAfter + 1
                    if (easterSundayNum == 2) feastName = "2. Niedziela Wielkanocna (Miłosierdzia Bożego)"
                    else if (easterSundayNum in 3..7) feastName = "$easterSundayNum. Niedziela Wielkanocna"
                }
                LiturgicalSeason.ORDINARY_TIME -> {
                    // Tu logika jest trudniejsza (zależna od Chrztu Pańskiego i Zesłania),
                    // na razie zostawmy "Niedziela Zwykła" lub dodamy później numerację
                    feastName = "Niedziela Zwykła"
                }
                else -> {}
            }
        }

        val sundayCycle = CycleCalculator.calculateSundayCycle(date, season)
        val weekdayCycle = CycleCalculator.calculateWeekdayCycle(date, season)

        return LiturgicalDay(
            date = date,
            season = season,
            feastName = feastName,
            isSolemnity = isSolemnity,
            sundayCycle = sundayCycle,
            weekdayCycle = weekdayCycle
        )
    }

    private fun calculateFirstAdventSunday(year: Int): LocalDate {
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val dowValue = christmas.dayOfWeek.value
        val daysToSubtract = if (dowValue == 7) 0 else dowValue
        val fourthAdvent = christmas.minusDays(daysToSubtract.toLong())
        return fourthAdvent.minusWeeks(3)
    }
}