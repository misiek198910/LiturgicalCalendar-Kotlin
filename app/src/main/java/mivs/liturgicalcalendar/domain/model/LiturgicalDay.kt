package mivs.liturgicalcalendar.domain.model

import mivs.liturgicalcalendar.domain.logic.CycleCalculator
import java.time.LocalDate

data class LiturgicalDay(
    val date: LocalDate,
    val season: LiturgicalSeason,
    val feastName: String? = null, // Np. "Środa Popielcowa", null dla dnia zwykłego
    val isSolemnity: Boolean = false, // Czy to Uroczystość? (ważne dla pierwszeństwa)
    val isFeast: Boolean = false, // Czy to Święto?
    val sundayCycle: CycleCalculator.SundayCycle,
    val weekdayCycle: CycleCalculator.WeekdayCycle,
    val feastKey: String? = null
) {
    // Pomocnicza flaga - czy dzień ma własną nazwę, czy jest "zwykły"
    val hasSpecialName: Boolean
        get() = feastName != null
}

