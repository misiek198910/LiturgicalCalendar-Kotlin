package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.entity.MovableFeastEntity

object MovableFeastsData {
    val list = listOf(
        // Środa Popielcowa (Zawsze te same czytania)
        MovableFeastEntity("ASH_WEDNESDAY", "MT 6, 1-6.16-18", "Zmiłuj się, Panie, bo jesteśmy grzeszni"),

        // Niedziela Palmowa (Rok A - pasuje do 2026)
        MovableFeastEntity("PALM_SUNDAY", "MT 26, 14 - 27, 66", "Boże mój, Boże, czemuś mnie opuścił?"),

        // Wielki Czwartek
        MovableFeastEntity("HOLY_THURSDAY", "J 13, 1-15", "Kielich Przymierza to Krew Zbawiciela"),

        // Wielki Piątek
        MovableFeastEntity("GOOD_FRIDAY", "J 18, 1 - 19, 42", "Ojcze, w Twoje ręce powierzam ducha mojego"),

        // Wielkanoc
        MovableFeastEntity("EASTER_SUNDAY", "J 20, 1-9", "W tym dniu wspaniałym wszyscy się weselmy"),

        // Poniedziałek Wielkanocny
        MovableFeastEntity("EASTER_MONDAY", "MT 28, 8-15", "Strzeż mnie, o Boże, Tobie zaufałem"),

        // Boże Ciało (Rok A)
        MovableFeastEntity("CORPUS_CHRISTI", "J 6, 51-58", "Kościele święty, chwal swojego Pana"),

        // Zesłanie Ducha Świętego
        MovableFeastEntity("PENTECOST", "J 20, 19-23", "Niech zstąpi Duch Twój i odnowi ziemię"),

        // Niedziela Miłosierdzia
        MovableFeastEntity("DIVINE_MERCY", "J 20, 19-31", "Dziękujcie Panu, bo jest miłosierny")
    )
}