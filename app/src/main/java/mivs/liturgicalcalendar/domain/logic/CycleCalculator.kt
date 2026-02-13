package mivs.liturgicalcalendar.domain.logic

import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.LocalDate

object CycleCalculator {

    enum class SundayCycle { A, B, C }
    enum class WeekdayCycle { I, II }

    fun calculateSundayCycle(date: LocalDate, season: LiturgicalSeason): SundayCycle {
        
        

        var liturgicalYear = date.year

        
        
        if (date.monthValue >= 11 && season == LiturgicalSeason.ADVENT) {
            liturgicalYear += 1
        }

        
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