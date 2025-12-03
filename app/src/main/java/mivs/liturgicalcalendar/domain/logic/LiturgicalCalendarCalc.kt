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

        // --- ZMIENNE POMOCNICZE (KLUCZ I NAZWA) ---
        var feastName: String? = null
        var isSolemnity = false
        var feastKey: String? = null

        // --- KOTWICE CZASOWE GŁÓWNE ---
        val ashWednesday = easter.minusDays(46)
        val pentecost = easter.plusWeeks(7)       // Zesłanie Ducha Św.
        val corpusChristi = easter.plusDays(60)   // Boże Ciało (Czwartek)

        // --- KOTWICE DODATKOWE (POLSKA SPECFIKA) ---
        // Wniebowstąpienie w Polsce: 7. Niedziela Wielkanocy (nie czwartek)
        val ascension = easter.plusWeeks(6)

        // Święta po Zesłaniu Ducha Świętego
        val maryMotherChurch = pentecost.plusDays(1) // Poniedziałek
        val jesusHighPriest = pentecost.plusDays(4)  // Czwartek
        val trinity = pentecost.plusWeeks(1)         // Niedziela Trójcy
        val sacredHeart = corpusChristi.plusDays(8)  // Piątek po oktawie Bożego Ciała
        val immaculateHeart = sacredHeart.plusDays(1)// Sobota po Sercu Jezusa

        // Adwent i Boże Narodzenie
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val firstAdvent = calculateFirstAdventSunday(year)
        val christKing = firstAdvent.minusWeeks(1) // Chrystusa Króla (tydzień przed Adwentem)

        // Święta w okresie Bożego Narodzenia
        val epiphany = LocalDate.of(year, Month.JANUARY, 6)
        val baptismOfLord = calculateBaptismOfLord(year)
        val holyFamily = calculateHolyFamily(year)


        // --- 1. DETEKCJA OKRESU (SEASON) ---
        var season = LiturgicalSeason.ORDINARY_TIME

        // Okres Bożego Narodzenia (do Chrztu Pańskiego włącznie)
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


        // --- 2. DETEKCJA KONKRETNYCH ŚWIĄT RUCHOMYCH ---

        // A. OKRES WIELKANOCNY I ZALEŻNE OD NIEGO
        if (date.isEqual(ashWednesday)) {
            feastName = "Środa Popielcowa"
            feastKey = "ASH_WEDNESDAY"
        }
        // Wielki Tydzień
        else if (date.isEqual(easter.minusWeeks(1))) {
            feastName = "Niedziela Palmowa Męki Pańskiej"
            feastKey = "PALM_SUNDAY"
            isSolemnity = true
        }
        else if (date.isEqual(easter.minusDays(6))) feastName = "Wielki Poniedziałek"
        else if (date.isEqual(easter.minusDays(5))) feastName = "Wielki Wtorek"
        else if (date.isEqual(easter.minusDays(4))) feastName = "Wielka Środa"
        else if (date.isEqual(easter.minusDays(3))) {
            feastName = "Wielki Czwartek"
            feastKey = "HOLY_THURSDAY"
        }
        else if (date.isEqual(easter.minusDays(2))) {
            feastName = "Wielki Piątek"
            feastKey = "GOOD_FRIDAY"
        }
        else if (date.isEqual(easter.minusDays(1))) {
            feastName = "Wielka Sobota"
            feastKey = "HOLY_SATURDAY"
        }
        // Wielkanoc i Oktawa
        else if (date.isEqual(easter)) {
            feastName = "Niedziela Zmartwychwstania Pańskiego"
            isSolemnity = true
            feastKey = "EASTER_SUNDAY"
        }
        else if (date.isEqual(easter.plusDays(1))) {
            feastName = "Poniedziałek w oktawie Wielkanocy"
            isSolemnity = true
            feastKey = "EASTER_MONDAY"
        }
        else if (date.isEqual(easter.plusDays(2))) feastName = "Wtorek w oktawie Wielkanocy"
        else if (date.isEqual(easter.plusDays(3))) feastName = "Środa w oktawie Wielkanocy"
        else if (date.isEqual(easter.plusDays(4))) feastName = "Czwartek w oktawie Wielkanocy"
        else if (date.isEqual(easter.plusDays(5))) feastName = "Piątek w oktawie Wielkanocy"
        else if (date.isEqual(easter.plusDays(6))) feastName = "Sobota w oktawie Wielkanocy"

        else if (date.isEqual(easter.plusWeeks(1))) {
            feastName = "Niedziela Wielkanocy, czyli Miłosierdzia Bożego"
            isSolemnity = true
            feastKey = "DIVINE_MERCY"
        }
        // Główne święta okresu wielkanocnego
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
        // Święta po Zesłaniu (Czas Zwykły, ale ruchome)
        else if (date.isEqual(maryMotherChurch)) {
            feastName = "Najświętszej Maryi Panny Matki Kościoła"
            isSolemnity = true
            feastKey = "MARY_MOTHER_CHURCH"
        }
        else if (date.isEqual(jesusHighPriest)) {
            feastName = "Jezusa Chrystusa, Najwyższego i Wiecznego Kapłana"
            isSolemnity = true
            feastKey = "JESUS_HIGH_PRIEST"
        }
        else if (date.isEqual(trinity)) {
            feastName = "Najświętszej Trójcy"
            isSolemnity = true
            feastKey = "HOLY_TRINITY"
        }
        else if (date.isEqual(corpusChristi)) {
            feastName = "Uroczystość Najświętszego Ciała i Krwi Chrystusa"
            isSolemnity = true
            feastKey = "CORPUS_CHRISTI"
        }
        else if (date.isEqual(sacredHeart)) {
            feastName = "Najświętszego Serca Pana Jezusa"
            isSolemnity = true
            feastKey = "SACRED_HEART"
        }
        else if (date.isEqual(immaculateHeart)) {
            feastName = "Niepokalanego Serca Najświętszej Maryi Panny"
            feastKey = "IMMACULATE_HEART" // Wspomnienie obowiązkowe
        }
        else if (date.isEqual(christKing)) {
            feastName = "Jezusa Chrystusa Króla Wszechświata"
            isSolemnity = true
            feastKey = "CHRIST_KING"
        }

        // B. OKRES BOŻEGO NARODZENIA (Ruchome)
        else if (date.isEqual(holyFamily)) {
            feastName = "Świętej Rodziny Jezusa, Maryi i Józefa"
            isSolemnity = true
            feastKey = "HOLY_FAMILY"
        }
        else if (date.isEqual(baptismOfLord)) {
            feastName = "Święto Chrztu Pańskiego"
            isSolemnity = true
            feastKey = "BAPTISM_OF_LORD"
        }

        // C. Święta stałe w algorytmie (jako fallback dla widoku miesiąca)
        else if (date.month == Month.DECEMBER && date.dayOfMonth == 25) {
            feastName = "Uroczystość Narodzenia Pańskiego"
            isSolemnity = true
        }
        else if (date.month == Month.JANUARY && date.dayOfMonth == 6) {
            feastName = "Objawienie Pańskie (Trzech Króli)"
            isSolemnity = true
        }


        // D. Logika dla NIEDZIEL (jeśli nazwa nie została nadana przez święto)
        if (feastName == null && date.dayOfWeek == DayOfWeek.SUNDAY) {
            isSolemnity = true

            when (season) {
                LiturgicalSeason.ADVENT -> {
                    val weekNum = ChronoUnit.WEEKS.between(firstAdvent, date).toInt() + 1
                    feastName = "$weekNum. Niedziela Adwentu"
                }
                LiturgicalSeason.LENT -> {
                    val weeksBeforeEaster = ChronoUnit.WEEKS.between(date, easter).toInt()
                    val lentSundayNum = 7 - weeksBeforeEaster
                    if (lentSundayNum in 1..5) {
                        feastName = "$lentSundayNum. Niedziela Wielkiego Postu"
                    }
                }
                LiturgicalSeason.EASTER -> {
                    val weeksAfter = ChronoUnit.WEEKS.between(easter, date).toInt()
                    val easterSundayNum = weeksAfter + 1
                    if (easterSundayNum in 3..7) feastName = "$easterSundayNum. Niedziela Wielkanocna"
                }
                LiturgicalSeason.CHRISTMAS -> {
                    // Niedziela między 2.01 a 5.01 to "2. Niedziela po Narodzeniu Pańskim"
                    if (date.dayOfMonth in 2..5 && date.month == Month.JANUARY) {
                        feastName = "2. Niedziela po Narodzeniu Pańskim"
                    }
                }
                LiturgicalSeason.ORDINARY_TIME -> {
                    // Tu w przyszłości można dodać numerację niedziel zwykłych
                    feastName = "Niedziela Zwykła"
                }
                else -> {} // Christmas time
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
            weekdayCycle = weekdayCycle,
            feastKey = feastKey
        )
    }

    // --- POMOCNICZE FUNKCJE KALENDARZOWE ---

    private fun calculateFirstAdventSunday(year: Int): LocalDate {
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        val dowValue = christmas.dayOfWeek.value
        // Jeśli BN to niedziela (7), to 4. niedziela adwentu to 18.12? Nie, to BN.
        // 4. Niedziela Adwentu to ostatnia niedziela PRZED 25.12.
        val daysToSubtract = if (dowValue == 7) 7 else dowValue // Cofamy się do niedzieli
        val fourthAdvent = christmas.minusDays(daysToSubtract.toLong())
        return fourthAdvent.minusWeeks(3)
    }

    // Święto Chrztu Pańskiego: Niedziela po 6 stycznia
    private fun calculateBaptismOfLord(year: Int): LocalDate {
        val epiphany = LocalDate.of(year, Month.JANUARY, 6)
        // Znajdź następną niedzielę
        // Jeśli 6.01 to Pon(1), chcemy dodać 6 dni -> 12.01
        // Jeśli 6.01 to Wt(2), chcemy dodać 5 dni -> 11.01 (TO JEST NASZ PRZYPADEK)
        // Jeśli 6.01 to Sob(6), chcemy dodać 1 dzień -> 7.01
        // Jeśli 6.01 to Niedz(7), w Polsce Chrzest jest w Poniedziałek 7.01 (ale to wyjątek)

        val daysToAdd = 7 - epiphany.dayOfWeek.value

        // Specjalny przypadek dla Polski: Jeśli Objawienie (6.01) wypada w Niedzielę,
        // to Chrzest Pański jest przeniesiony na Poniedziałek (7.01).
        if (daysToAdd == 0) return epiphany.plusDays(1)

        return epiphany.plusDays(daysToAdd.toLong())
    }

    // Święta Rodzina: Niedziela w oktawie BN (26-31.12) lub 30.12 jeśli BN to niedziela
    private fun calculateHolyFamily(year: Int): LocalDate {
        val christmas = LocalDate.of(year, Month.DECEMBER, 25)
        if (christmas.dayOfWeek == DayOfWeek.SUNDAY) {
            // Jeśli BN to niedziela, Św. Rodzina jest w piątek 30.12
            return LocalDate.of(year, Month.DECEMBER, 30)
        } else {
            // Znajdź niedzielę po 25.12
            val daysToAdd = 7 - christmas.dayOfWeek.value
            return christmas.plusDays(daysToAdd.toLong())
        }
    }

}