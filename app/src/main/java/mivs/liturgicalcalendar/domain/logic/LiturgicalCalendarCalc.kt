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
        val goodFriday = easter.minusDays(2)
        val ascension = easter.plusDays(42)
        val pentecost = easter.plusWeeks(7)
        val corpusChristi = easter.plusDays(60)

        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val firstAdvent = calculateFirstAdventSunday(year)

        // Epifania (6 stycznia)
        val epiphany = LocalDate.of(year, Month.JANUARY, 6)

        // POPRAWKA: Obliczanie daty Chrztu Pańskiego
        // Jeśli 6.01 to Niedziela -> Chrzest jest w poniedziałek 7.01
        // W przeciwnym razie -> Chrzest jest w następną Niedzielę po 6.01
        var baptismOfLord: LocalDate = if (epiphany.dayOfWeek == DayOfWeek.SUNDAY) {
            epiphany.plusDays(1)
        } else {
            val daysToSunday = 7 - (epiphany.dayOfWeek.value % 7)
            epiphany.plusDays(daysToSunday.toLong())
        }

        val christKing = firstAdvent.minusWeeks(1)

        // Obliczanie Świętej Rodziny (Niedziela w oktawie lub 30.12)
        val holyFamily = if (christmas.dayOfWeek == DayOfWeek.SUNDAY) {
            LocalDate.of(year, Month.DECEMBER, 30)
        } else {
            christmas.plusDays((7 - christmas.dayOfWeek.value % 7).toLong())
        }

        // --- 1. DETEKCJA OKRESU (SEASON) ---
        var season = LiturgicalSeason.ORDINARY_TIME

        if ((date.month == Month.DECEMBER && date.dayOfMonth >= 25) ||
            (date.year == year && !date.isAfter(baptismOfLord))) {
            // Okres Bożego Narodzenia trwa do święta Chrztu Pańskiego włącznie
            season = LiturgicalSeason.CHRISTMAS
        }
        else if (!date.isBefore(ashWednesday) && date.isBefore(easter)) {
            if (date.isEqual(goodFriday) || date.isEqual(easter.minusDays(1))) {
                season = LiturgicalSeason.TRIDUUM
            } else {
                season = LiturgicalSeason.LENT
            }
        }
        else if (!date.isBefore(easter) && !date.isAfter(pentecost)) {
            season = LiturgicalSeason.EASTER
        }
        else if (!date.isBefore(firstAdvent) && date.isBefore(christmas)) {
            season = LiturgicalSeason.ADVENT
        }

        var feastName: String? = null
        var isSolemnity = false
        var feastKey: String? = null

        if (date.isEqual(ashWednesday)) {
            feastName = "Środa Popielcowa"
            feastKey = "ASH_WEDNESDAY"
        }
        else if (date.isEqual(goodFriday)) {
            feastName = "Wielki Piątek Męki Pańskiej"
            isSolemnity = true
            feastKey = "GOOD_FRIDAY"
        }
        else if (date.isEqual(easter)) {
            feastName = "Niedziela Zmartwychwstania Pańskiego"
            isSolemnity = true
            feastKey = "EASTER_SUNDAY"
        }
        else if (date.isEqual(ascension)) {
            feastName = "Wniebowstąpienie Pańskie"
            isSolemnity = true
            feastKey = "ASCENSION"
        }
        else if (date.isEqual(pentecost)) {
            feastName = "Niedziela Zesłania Ducha Świętego"
            isSolemnity = true
            feastKey = "PENTECOST"
        }
        else if (date.isEqual(corpusChristi)) {
            feastName = "Uroczystość Najświętszego Ciała i Krwi Chrystusa"
            isSolemnity = true
            feastKey = "CORPUS_CHRISTI"
        }
        else if (date.isEqual(epiphany)) {
            // Epifania jest w bazie stałej
        }
        else if (date.isEqual(baptismOfLord)) {
            feastName = "Święto Chrztu Pańskiego"
            feastKey = "BAPTISM_OF_LORD"
        }
        else if (date.isEqual(christKing)) {
            feastName = "Uroczystość Jezusa Chrystusa, Króla Wszechświata"
            isSolemnity = true
            feastKey = "CHRIST_KING"
        }
        else if (date.isEqual(holyFamily)) {
            feastName = "Święto Świętej Rodziny Jezusa, Maryi i Józefa"
            isSolemnity = true
            feastKey = "HOLY_FAMILY"
        }

        val sundayCycle = CycleCalculator.calculateSundayCycle(date, season)
        val weekdayCycle = CycleCalculator.calculateWeekdayCycle(date, season)

        // --- OBLICZANIE KLUCZA LEKCJONARZA ---
        val lectionaryKey = if (feastKey == "HOLY_FAMILY") {
            "HOLY_FAMILY_$sundayCycle"
        } else if (feastKey == null || date.isEqual(baptismOfLord)) {
            // Chrzest Pański wchodzi do kalkulacji, by dostać klucz "ORD_SUN_1_..."
            calculateLectionaryKey(date, season, firstAdvent, ashWednesday, easter, sundayCycle, baptismOfLord, christKing)
        } else null


        if (feastName == null && date.dayOfWeek == DayOfWeek.SUNDAY) {
            when (season) {
                LiturgicalSeason.ADVENT -> {
                    val week = (ChronoUnit.DAYS.between(firstAdvent, date) / 7).toInt() + 1
                    feastName = "$week. Niedziela Adwentu"
                }
                LiturgicalSeason.LENT -> {
                    val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                    val week = (daysFromAsh / 7).toInt() + 1
                    feastName = if (week == 6) "Niedziela Palmowa" else "$week. Niedziela Wielkiego Postu"
                }
                LiturgicalSeason.EASTER -> {
                    val week = (ChronoUnit.DAYS.between(easter, date) / 7).toInt() + 1
                    feastName = when (week) {
                        2 -> "2. Niedziela Wielkanocna (Miłosierdzia Bożego)"
                        else -> "$week. Niedziela Wielkanocna"
                    }
                }
                LiturgicalSeason.ORDINARY_TIME -> {
                    val parts = lectionaryKey?.split("_")
                    if (parts != null && parts.size >= 3) {
                        val weekNum = parts[2].toIntOrNull()
                        if (weekNum != null) feastName = "$weekNum. Niedziela Zwykła"
                    }
                }
                else -> {}
            }
        }

        return LiturgicalDay(
            date = date,
            season = season,
            feastName = feastName,
            isSolemnity = isSolemnity,
            sundayCycle = sundayCycle,
            weekdayCycle = weekdayCycle,
            feastKey = feastKey,
            lectionaryKey = lectionaryKey
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
        val dow = date.dayOfWeek.name.take(3)

        return when (season) {
            LiturgicalSeason.ADVENT -> {
                val weekNum = (ChronoUnit.DAYS.between(adventStart, date) / 7).toInt() + 1
                if (dow == "SUN") "ADVENT_SUN_${weekNum}_$cycle" else "ADVENT_W${weekNum}_$dow"
            }

            LiturgicalSeason.LENT, LiturgicalSeason.TRIDUUM -> {
                val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                if (daysFromAsh < 4) "LENT_ASH_$dow"
                else {
                    // POPRAWKA: Liczenie tygodni od Niedzieli (odejmujemy 4 dni: śr-sob)
                    val weekNum = ((daysFromAsh - 4) / 7).toInt() + 1

                    if (weekNum == 6) {
                        if (dow == "SUN") "LENT_SUN_6_$cycle" // Niedziela Palmowa
                        else "HOLY_WEEK_$dow" // Wielki Poniedziałek, Wtorek...
                    } else {
                        if (dow == "SUN") "LENT_SUN_${weekNum}_$cycle" else "LENT_W${weekNum}_$dow"
                    }
                }
            }

            LiturgicalSeason.EASTER -> {
                val weekNum = (ChronoUnit.DAYS.between(easter, date) / 7).toInt() + 1
                if (dow == "SUN") "EASTER_SUN_${weekNum}_$cycle" else "EASTER_W${weekNum}_$dow"
            }

            LiturgicalSeason.CHRISTMAS -> {
                if (date.month == Month.DECEMBER && date.dayOfMonth > 25) {
                    val dayOfOctave = date.dayOfMonth - 25 + 1
                    "CHRISTMAS_OCTAVE_$dayOfOctave"
                }
                else if (date.month == Month.JANUARY) {
                    if (date.dayOfMonth in 2..5) {
                        "CHRISTMAS_JAN_${date.dayOfMonth}"
                    }
                    else if (date.isEqual(baptismOfLord)) {
                        "ORD_SUN_1_$cycle"
                    }
                    else if (date.dayOfMonth >= 7 && date.isBefore(baptismOfLord)) {
                        "CHRISTMAS_AFTER_EPIPHANY_$dow"
                    } else null
                } else null
            }

            LiturgicalSeason.ORDINARY_TIME -> {
                var weekNum = 0
                if (date.isBefore(ashWednesday)) {
                    val daysFromBaptism = ChronoUnit.DAYS.between(baptismOfLord, date)
                    weekNum = (daysFromBaptism / 7).toInt() + 1
                }
                else {
                    // POPRAWKA: Liczenie tygodni od końca roku liturgicznego (Christ King)
                    // Dodajemy 6 dni, aby "zaokrąglić" do pełnego tygodnia w górę
                    val daysToEnd = ChronoUnit.DAYS.between(date, christKing)
                    val weeksFromEnd = ((daysToEnd + 6) / 7).toInt()
                    weekNum = 34 - weeksFromEnd
                }

                if (weekNum in 1..34) {
                    if (dow == "SUN") "ORD_SUN_${weekNum}_$cycle"
                    else "ORD_W${weekNum}_$dow"
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