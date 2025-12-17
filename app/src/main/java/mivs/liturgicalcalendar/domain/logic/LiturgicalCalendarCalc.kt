package mivs.liturgicalcalendar.domain.logic

import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object LiturgicalCalendarCalc {

    // --- FUNKCJE POMOCNICZE ---

    private fun getFeriaCycle(yearCycle: Char): Int {
        return when (yearCycle) {
            'A', 'C' -> 1
            'B' -> 2
            else -> 1
        }
    }

    private fun calculateFirstAdventSunday(year: Int): LocalDate {
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val fourthAdvent = christmas.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return fourthAdvent.minusWeeks(3)
    }

    private fun getPolishDayName(dow: String): String {
        return when (dow.toUpperCase(Locale.ROOT)) {
            "MON" -> "Poniedziałek"
            "TUE" -> "Wtorek"
            "WED" -> "Środa"
            "THU" -> "Czwartek"
            "FRI" -> "Piątek"
            "SAT" -> "Sobota"
            "SUN" -> "Niedziela"
            else -> dow
        }
    }

    // --- LOGIKA GŁÓWNA ---

    fun generateDay(date: LocalDate): LiturgicalDay {
        val currentYear = date.year
        val dow = date.dayOfWeek.name.take(3)

        val liturgicalYear = if (date.monthValue <= 1 && date.dayOfMonth < 12) currentYear - 1 else currentYear

        val easter = EasterCalculator.calculate(currentYear)
        val ashWednesday = easter.minusDays(46)
        val goodFriday = easter.minusDays(2)
        val ascension = easter.plusDays(39)
        val pentecost = easter.plusWeeks(7)
        val corpusChristi = easter.plusDays(60)

        val christmasDate = LocalDate.of(liturgicalYear, Month.DECEMBER, 25)
        val firstAdvent = calculateFirstAdventSunday(liturgicalYear)
        val christKing = firstAdvent.minusWeeks(1)

        val seasonYear = if (date.month == Month.DECEMBER) currentYear + 1 else currentYear
        val epiphanySeason = LocalDate.of(seasonYear, Month.JANUARY, 6)
        var baptismOfLordSeason = epiphanySeason.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        if (epiphanySeason.dayOfWeek == DayOfWeek.SUNDAY) {
            baptismOfLordSeason = epiphanySeason.plusDays(1)
        }

        val epiphany = LocalDate.of(currentYear, Month.JANUARY, 6)
        var baptismOfLord = epiphany.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        if (epiphany.dayOfWeek == DayOfWeek.SUNDAY) {
            baptismOfLord = epiphany.plusDays(1)
        }

        val holyFamily: LocalDate = when {
            christmasDate.dayOfWeek == DayOfWeek.SUNDAY -> LocalDate.of(christmasDate.year, Month.DECEMBER, 30)
            christmasDate.dayOfWeek == DayOfWeek.SATURDAY -> LocalDate.of(christmasDate.year + 1, Month.JANUARY, 1)
            else -> {
                christmasDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
        }

        val sundayCycle = CycleCalculator.calculateSundayCycle(date, LiturgicalSeason.ORDINARY_TIME)
        val sundayCycleChar = sundayCycle.name.last()
        val feriaCycleNum = getFeriaCycle(sundayCycleChar)

        val season = when {
            date.isEqual(easter.minusDays(3)) || date.isEqual(goodFriday) || date.isEqual(easter.minusDays(1)) -> LiturgicalSeason.TRIDUUM
            !date.isBefore(easter) && date.isBefore(pentecost.plusDays(1)) -> LiturgicalSeason.EASTER
            !date.isBefore(ashWednesday) && date.isBefore(easter) -> LiturgicalSeason.LENT
            date.month == Month.DECEMBER && date.dayOfMonth == 24 -> LiturgicalSeason.ADVENT
            !date.isBefore(firstAdvent) && date.isBefore(christmasDate) -> LiturgicalSeason.ADVENT
            date.isAfter(christmasDate.minusDays(1)) && date.isBefore(baptismOfLordSeason.plusDays(1)) -> LiturgicalSeason.CHRISTMAS
            else -> LiturgicalSeason.ORDINARY_TIME
        }

        var feastName: String? = null
        var isSolemnity = false
        var feastKey: String? = null
        var colorCode = "g"

        // Ustawianie kolorów
        colorCode = when(season) {
            LiturgicalSeason.ADVENT -> "v"
            LiturgicalSeason.LENT -> "v"
            LiturgicalSeason.CHRISTMAS -> "w"
            LiturgicalSeason.EASTER -> "w"
            LiturgicalSeason.TRIDUUM -> if(date.isEqual(goodFriday)) "r" else "w"
            else -> "g"
        }

        // A. Święta Ruchome
        if (date.month == Month.DECEMBER && date.dayOfMonth == 24) {
            feastKey = "CHRISTMAS_EVE"
            colorCode = "w"
        } else if (date.isEqual(LocalDate.of(currentYear, Month.JANUARY, 1))) {
            feastName = "Świętej Bożej Rodzicielki Maryi"; isSolemnity = true; feastKey = "NEW_YEAR"; colorCode = "w"
        } else if (date.isEqual(ashWednesday)) {
            feastName = "Środa Popielcowa"; feastKey = "ASH_WEDNESDAY"; colorCode = "v"
        } else if (date.isEqual(goodFriday)) {
            feastName = "Wielki Piątek Męki Pańskiej"; isSolemnity = true; feastKey = "GOOD_FRIDAY"; colorCode = "r"
        } else if (date.isEqual(easter)) {
            feastName = "Niedziela Zmartwychwstania Pańskiego"; isSolemnity = true; feastKey = "EASTER_SUNDAY"; colorCode = "w"
        } else if (date.isEqual(easter.plusDays(1))) {
            feastName = "Poniedziałek Wielkanocny"; feastKey = "EASTER_MONDAY"; colorCode = "w"
        } else if (date.isEqual(easter.plusDays(7))) {
            feastName = "Niedziela Miłosierdzia Bożego"; isSolemnity = true; feastKey = "DIVINE_MERCY"; colorCode = "w"
        } else if (date.isEqual(ascension)) {
            feastName = "Wniebowstąpienie Pańskie"; isSolemnity = true; feastKey = "ASCENSION"; colorCode = "w"
        } else if (date.isEqual(pentecost)) {
            feastName = "Niedziela Zesłania Ducha Świętego"; isSolemnity = true; feastKey = "PENTECOST"; colorCode = "r"
        } else if (date.isEqual(corpusChristi)) {
            feastName = "Uroczystość Najświętszego Ciała i Krwi Chrystusa"; isSolemnity = true; feastKey = "CORPUS_CHRISTI"; colorCode = "w"
        } else if (date.isEqual(christKing)) {
            feastName = "Uroczystość Jezusa Chrystusa, Króla Wszechświata"; isSolemnity = true; feastKey = "CHRIST_KING"; colorCode = "w"
        } else if (date.isEqual(baptismOfLord)) {
            feastName = "Święto Chrztu Pańskiego"; feastKey = "BAPTISM_OF_LORD"; colorCode = "w"
        } else if (date.isEqual(holyFamily)) {
            feastName = "Święto Świętej Rodziny Jezusa, Maryi i Józefa"; isSolemnity = true; feastKey = "HOLY_FAMILY"; colorCode = "w"
        }

        var lectionaryKey = calculateLectionaryKey(date, season, firstAdvent, ashWednesday, easter, sundayCycleChar, feriaCycleNum, baptismOfLord, christKing, pentecost)

        if (feastKey != null) {
            lectionaryKey = when (feastKey) {
                "CHRISTMAS_EVE" -> "CHRISTMAS_EVE"
                "HOLY_FAMILY" -> "HOLY_FAMILY_$sundayCycleChar"
                "CHRIST_KING" -> "ORD_SUN_34_$sundayCycleChar"
                "BAPTISM_OF_LORD" -> "ORD_SUN_1_$sundayCycleChar"
                "DIVINE_MERCY" -> "EASTER_SUN_2_$sundayCycleChar"
                "EASTER_MONDAY" -> "EASTER_W1_MON"
                "NEW_YEAR" -> "NEW_YEAR"
                "ASH_WEDNESDAY" -> "LENT_ASH_WED"
                else -> feastKey
            }
        }

        // 4. USTALANIE NAZW DNI
        if (feastName == null) {
            if (date.month == Month.DECEMBER && date.dayOfMonth == 24) {
                feastName = "Wigilia Bożego Narodzenia"
            }
            else if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                if (season == LiturgicalSeason.ADVENT) {
                    val week = (ChronoUnit.DAYS.between(firstAdvent, date) / 7).toInt() + 1
                    feastName = "$week. Niedziela Adwentu"
                } else if (season == LiturgicalSeason.LENT) {
                    val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                    val week = (daysFromAsh / 7).toInt() + 1
                    feastName = if (week == 6) "Niedziela Palmowa" else "$week. Niedziela Wielkiego Postu"
                } else if (season == LiturgicalSeason.EASTER) {
                    val week = (ChronoUnit.DAYS.between(easter, date) / 7).toInt() + 1
                    feastName = "$week. Niedziela Wielkanocna"
                } else if (season == LiturgicalSeason.CHRISTMAS) {
                    feastName = "Niedziela w Okresie Narodzenia Pańskiego"
                } else if (season == LiturgicalSeason.ORDINARY_TIME) {
                    var baseDate: LocalDate
                    var weekOffset: Int
                    if (date.isBefore(ashWednesday)) {
                        baseDate = baptismOfLord.plusDays(1)
                        weekOffset = 2
                    } else {
                        baseDate = pentecost.plusDays(1)
                        weekOffset = 9
                    }
                    val daysFromBase = ChronoUnit.DAYS.between(baseDate, date)
                    var weekNum = (daysFromBase / 7).toInt() + weekOffset
                    if (weekNum > 34) weekNum = 34
                    if (weekNum == 1) weekNum = 2

                    feastName = if (weekNum in 1..34) "$weekNum. Niedziela Zwykła" else "Niedziela Zwykła"
                }
            }
            else if (date.dayOfWeek != DayOfWeek.SUNDAY) {
                if (season == LiturgicalSeason.ORDINARY_TIME) {
                    val weekNumMatch = lectionaryKey?.split("_")?.getOrNull(1)?.removePrefix("W")?.toIntOrNull()
                    if (weekNumMatch != null) feastName = "${getPolishDayName(dow)} $weekNumMatch. Tygodnia Zwykłego"
                } else if (season == LiturgicalSeason.ADVENT) {
                    val weekNum = (ChronoUnit.DAYS.between(firstAdvent, date) / 7).toInt() + 1
                    feastName = "${getPolishDayName(dow)} $weekNum. Tygodnia Adwentu"
                } else if (season == LiturgicalSeason.CHRISTMAS) {
                    if (date.month == Month.DECEMBER && date.dayOfMonth in 26..31) {
                        val octaveDayNum = date.dayOfMonth - 25 + 1
                        val romanNum = when(octaveDayNum) { 2->"II"; 3->"III"; 4->"IV"; 5->"V"; 6->"VI"; 7->"VII"; else->"$octaveDayNum" }
                        feastName = "${getPolishDayName(dow)}, $romanNum dzień w Oktawie Narodzenia Pańskiego"
                    }
                    else if (date.month == Month.JANUARY && date.dayOfMonth in 7..10) {
                        feastName = "${getPolishDayName(dow)} po Epifanii"
                    } else if (date.month == Month.JANUARY && date.dayOfMonth in 2..5) {
                        feastName = "${getPolishDayName(dow)} w Okresie Narodzenia Pańskiego"
                    }
                } else if (season == LiturgicalSeason.TRIDUUM) {
                    if (date.isEqual(easter.minusDays(3))) feastName = "Wielki Czwartek: Wieczerzy Pańskiej"
                    else if (date.isEqual(easter.minusDays(2))) feastName = "Wielki Piątek: Męki Pańskiej"
                    else if (date.isEqual(easter.minusDays(1))) feastName = "Wielka Sobota"
                } else if (season == LiturgicalSeason.LENT) {
                    // --- DEBUGOWANIE WIELKIEGO POSTU ---
                    val daysToEaster = ChronoUnit.DAYS.between(date, easter)
                    val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                    val weekNum = (daysFromAsh / 7).toInt() + 1

                    println("DEBUG_LENT: Data=$date, Wielkanoc=$easter, DniDoWielkanocy=$daysToEaster, NumerTygodnia=$weekNum")

                    if (daysToEaster in 1..7) {
                        println("DEBUG_LENT: Wykryto Wielki Tydzień dla $date")
                        val holyDayName = when(dow) {
                            "MON" -> "Wielki Poniedziałek"
                            "TUE" -> "Wielki Wtorek"
                            "WED" -> "Wielka Środa"
                            "THU" -> "Wielki Czwartek"
                            "SUN" -> "Niedziela Palmowa Męki Pańskiej"
                            else -> "Wielki Tydzień"
                        }
                        feastName = holyDayName
                    } else {
                        println("DEBUG_LENT: Wykryto ZWYKŁY tydzień postu dla $date")
                        feastName = "${getPolishDayName(dow)} $weekNum. Tygodnia Wielkiego Postu"
                    }
                    // -----------------------------------
                } else if (season == LiturgicalSeason.EASTER) {
                    val daysFromEaster = ChronoUnit.DAYS.between(easter, date)
                    val weekNum = (daysFromEaster / 7).toInt() + 1

                    if (weekNum == 1) {
                        feastName = "${getPolishDayName(dow)} w Oktawie Wielkanocy"
                    } else {
                        feastName = "${getPolishDayName(dow)} $weekNum. Tygodnia Wielkanocnego"
                    }
                }
            }
        }

        return LiturgicalDay(
            date = date,
            season = season,
            feastName = feastName,
            isSolemnity = isSolemnity,
            sundayCycle = sundayCycle,
            weekdayCycle = CycleCalculator.calculateWeekdayCycle(date, season),
            feastKey = feastKey,
            lectionaryKey = lectionaryKey,
            colorCode = colorCode
        )
    }

    private fun calculateLectionaryKey(
        date: LocalDate,
        season: LiturgicalSeason,
        adventStart: LocalDate,
        ashWednesday: LocalDate,
        easter: LocalDate,
        cycle: Char,
        feriaCycle: Int,
        baptismOfLord: LocalDate,
        christKing: LocalDate,
        pentecost: LocalDate
    ): String? {
        val dow = date.dayOfWeek.name.take(3)
        return when (season) {
            LiturgicalSeason.ADVENT -> {
                if (date.month == Month.DECEMBER && date.dayOfMonth == 24) return "ADVENT_W4_WED_CHRISTMAS_EVE"
                val daysFromAdventStart = ChronoUnit.DAYS.between(adventStart, date)
                val weekNum = (daysFromAdventStart / 7).toInt() + 1
                if (dow == "SUN") if (weekNum <= 4) "ADVENT_SUN_${weekNum}_$cycle" else "ADVENT_SUN_4_$cycle"
                else "ADVENT_W${weekNum}_$dow"
            }
            LiturgicalSeason.LENT, LiturgicalSeason.TRIDUUM -> {
                val daysFromAsh = ChronoUnit.DAYS.between(ashWednesday, date)
                val daysToEaster = ChronoUnit.DAYS.between(date, easter)

                if (dow == "SUN") {
                    val weekNum = (daysFromAsh / 7).toInt() + 1
                    if (weekNum == 6) "LENT_SUN_6_$cycle" else "LENT_SUN_${weekNum}_$cycle"
                } else {
                    if (daysToEaster == 3L) return "HOLY_THURSDAY"
                    if (daysToEaster == 2L) return "GOOD_FRIDAY"
                    if (daysToEaster == 1L) return "HOLY_SATURDAY"

                    // Klucze dla Wielkiego Tygodnia
                    if (daysToEaster < 7) {
                        return "HOLY_WEEK_$dow"
                    }

                    if (daysFromAsh < 4) "LENT_ASH_$dow"
                    else {
                        val weekNum = ((daysFromAsh - 4) / 7).toInt() + 1
                        "LENT_W${weekNum}_$dow"
                    }
                }
            }
            LiturgicalSeason.EASTER -> {
                val daysFromEaster = ChronoUnit.DAYS.between(easter, date)
                val weekNum = (daysFromEaster / 7).toInt() + 1
                if (dow == "SUN") "EASTER_SUN_${weekNum}_$cycle" else "EASTER_W${weekNum}_$dow"
            }
            LiturgicalSeason.CHRISTMAS -> {
                if (date.isEqual(LocalDate.of(date.year, Month.JANUARY, 1))) return "NEW_YEAR"
                if (dow == "SUN") {
                    if (date.isEqual(baptismOfLord)) return "ORD_SUN_1_$cycle"
                    return "CHRISTMAS_SUN"
                }
                if (date.month == Month.DECEMBER) {
                    when (date.dayOfMonth) {
                        26 -> "CHRISTMAS_DEC_26"
                        27 -> "CHRISTMAS_DEC_27"
                        28 -> "CHRISTMAS_DEC_28"
                        in 29..31 -> "CHRISTMAS_OCTAVE_${date.dayOfMonth - 25 + 1}"
                        else -> null
                    }
                } else if (date.month == Month.JANUARY) {
                    when (date.dayOfMonth) {
                        in 2..5 -> "CHRISTMAS_JAN_${date.dayOfMonth}"
                        6 -> "EPIPHANY"
                        in 7..13 -> if (date.isBefore(baptismOfLord)) "CHRISTMAS_AFTER_EPIPHANY_$dow" else null
                        else -> null
                    }
                } else null
            }
            LiturgicalSeason.ORDINARY_TIME -> {
                var weekNum = 0
                if (date.isBefore(ashWednesday)) {
                    if (date.isAfter(baptismOfLord)) {
                        val daysSinceBaptism = ChronoUnit.DAYS.between(baptismOfLord.plusDays(1), date)
                        weekNum = (daysSinceBaptism / 7).toInt() + 2
                    } else if (date.isEqual(baptismOfLord)) weekNum = 1
                } else {
                    val weeksToAdvent = (ChronoUnit.DAYS.between(date, adventStart.withYear(date.year)) / 7).toInt()
                    weekNum = 34 - weeksToAdvent
                    if (weekNum < 9) weekNum = 9
                    if (date.isAfter(christKing)) weekNum = 34
                }
                if (weekNum in 1..34) {
                    if (dow == "SUN") "ORD_SUN_${weekNum}_$cycle" else "ORD_W${weekNum}_${dow}_$feriaCycle"
                } else null
            }
            else -> null
        }
    }
}