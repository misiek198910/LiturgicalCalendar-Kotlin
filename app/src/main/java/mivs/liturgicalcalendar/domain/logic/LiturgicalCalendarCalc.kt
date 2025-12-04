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

        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val firstAdvent = calculateFirstAdventSunday(year)

        // Do obliczania tygodni zwykłych
        val epiphany = LocalDate.of(year, Month.JANUARY, 6)
        // Chrzest Pański: Niedziela po 6.01 (chyba że 6.01 to niedziela, wtedy poniedziałek)
        var baptismOfLord = epiphany.plusDays((7 - epiphany.dayOfWeek.value % 7).toLong() + 1)
        if (epiphany.dayOfWeek == DayOfWeek.SUNDAY) baptismOfLord = epiphany.plusDays(1)

        val christKing = firstAdvent.minusWeeks(1)

        // --- 1. DETEKCJA OKRESU (SEASON) ---
        var season = LiturgicalSeason.ORDINARY_TIME

        if ((date.month == Month.DECEMBER && date.dayOfMonth >= 25) ||
            (date.year == year && !date.isAfter(baptismOfLord))) {
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

        // --- 2. DETEKCJA ŚWIĄT (Uproszczona dla generatora) ---
        var feastName: String? = null
        var isSolemnity = false
        var feastKey: String? = null

        // (Tu normalnie są ify dla świąt ruchomych, ale dla generatora dni zwykłych
        // najważniejsze jest, żeby nie nadpisać ich, jeśli to nie święto)

        // --- 3. OBLICZANIE KLUCZA LEKCJONARZA ---
        // To jest to, czego brakowało!
        val sundayCycle = CycleCalculator.calculateSundayCycle(date, season)
        val weekdayCycle = CycleCalculator.calculateWeekdayCycle(date, season)

        // Jeśli to nie jest święto ruchome (feastKey == null), oblicz klucz dnia
        val lectionaryKey = if (feastKey == null) {
            calculateLectionaryKey(date, season, firstAdvent, ashWednesday, easter, sundayCycle, baptismOfLord, christKing)
        } else null

        return LiturgicalDay(
            date = date,
            season = season,
            feastName = feastName,
            isSolemnity = isSolemnity,
            sundayCycle = sundayCycle,
            weekdayCycle = weekdayCycle,
            feastKey = feastKey,
            lectionaryKey = lectionaryKey // <--- Przekazujemy wyliczony klucz
        )
    }

    private fun calculateLectionaryKey(
        date: LocalDate,
        season: LiturgicalSeason,
        adventStart: LocalDate,
        ashWednesday: LocalDate,
        easter: LocalDate,
        cycle: CycleCalculator.SundayCycle,
        baptismOfLord: LocalDate,
        christKing: LocalDate
    ): String? {
        val dow = date.dayOfWeek.name.take(3) // MON, TUE...

        // Ważne dla dni zwykłych: Cykl tygodnia (nie niedzieli)
        // W 2025 (rok nieparzysty) mamy Cykl I. W 2026 Cykl II.
        // Ale Ewangelie są te same, więc generator wygeneruje klucze bez sufiksu I/II,
        // albo zignorujemy sufiks w mapie, jeśli czytania są wspólne.
        // Tutaj generujemy uniwersalny klucz dla Ewangelii: ORD_W3_MON

        return when (season) {
            LiturgicalSeason.ADVENT -> {
                val weekNum = (ChronoUnit.DAYS.between(adventStart, date) / 7).toInt() + 1
                if (dow == "SUN") "ADVENT_SUN_${weekNum}_$cycle" else "ADVENT_W${weekNum}_$dow"
            }
            LiturgicalSeason.LENT -> {
                val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                if (daysFromAsh < 4) "LENT_ASH_$dow"
                else {
                    val weekNum = (daysFromAsh / 7).toInt() + 1
                    if (dow == "SUN") "LENT_SUN_${weekNum}_$cycle" else "LENT_W${weekNum}_$dow"
                }
            }
            LiturgicalSeason.EASTER -> {
                val weekNum = (ChronoUnit.DAYS.between(easter, date) / 7).toInt() + 1
                if (dow == "SUN") "EASTER_SUN_${weekNum}_$cycle" else "EASTER_W${weekNum}_$dow"
            }
            LiturgicalSeason.ORDINARY_TIME -> {
                var weekNum = 0

                // Część 1: Od Chrztu do Postu
                if (date.isBefore(ashWednesday)) {
                    // Tydzień 1 zaczyna się po Chrzcie Pańskim
                    val daysFromBaptism = ChronoUnit.DAYS.between(baptismOfLord, date)
                    weekNum = (daysFromBaptism / 7).toInt() + 1
                }
                // Część 2: Od Zesłania do Adwentu
                else {
                    // Liczymy WSTECZ od Chrystusa Króla (34. tydzień)
                    // Chrystus Król to niedziela kończąca 34. tydzień.
                    // Data to niedziela przed Adwentem.
                    val weeksFromEnd = (ChronoUnit.DAYS.between(date, christKing) / 7).toInt()
                    weekNum = 34 - weeksFromEnd
                }

                if (weekNum in 1..34) {
                    if (dow == "SUN") "ORD_SUN_${weekNum}_$cycle"
                    else "ORD_W${weekNum}_$dow" // Np. ORD_W3_MON (bez I/II, bo ewangelie te same)
                } else null
            }
            else -> null
        }
    }

    private fun calculateFirstAdventSunday(year: Int): LocalDate {
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val dowValue = christmas.dayOfWeek.value
        val daysToSubtract = if (dowValue == 7) 7 else dowValue
        val fourthAdvent = christmas.minusDays(daysToSubtract.toLong())
        return fourthAdvent.minusWeeks(3)
    }
}