package mivs.liturgicalcalendar.domain.logic

import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.LocalDate

object CycleCalculator {

    enum class SundayCycle { A, B, C }
    enum class WeekdayCycle { I, II }

    fun calculateSundayCycle(date: LocalDate, season: LiturgicalSeason): SundayCycle {
        // Obliczamy "rok liturgiczny". Zaczyna się on w 1. Niedzielę Adwentu poprzedniego roku kalendarzowego.
        // Np. Adwent 2025 zaczyna rok liturgiczny 2026.

        var liturgicalYear = date.year

        // Jeśli data jest w Adwencie (lub po nim w grudniu), to należy już do kolejnego roku liturgicznego
        // Uproszczenie: Jeśli miesiąc to grudzień lub koniec listopada i sezon to ADWENT -> rok + 1
        if (date.monthValue >= 11 && season == LiturgicalSeason.ADVENT) {
            liturgicalYear += 1
        }

        // Algorytm: Reszta z dzielenia przez 3
        return when (liturgicalYear % 3) {
            1 -> SundayCycle.A
            2 -> SundayCycle.B
            0 -> SundayCycle.C
            else -> SundayCycle.A
        }
    }

    fun calculateWeekdayCycle(date: LocalDate, season: LiturgicalSeason): WeekdayCycle {
        var liturgicalYear = date.year
        if (date.monthValue >= 11 && season == LiturgicalSeason.ADVENT) {
            liturgicalYear += 1
        }

        return if (liturgicalYear % 2 == 1) WeekdayCycle.I else WeekdayCycle.II
    }
}