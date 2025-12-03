package mivs.liturgicalcalendar.domain.model

import mivs.liturgicalcalendar.domain.logic.CycleCalculator
import java.time.LocalDate

data class LiturgicalDay(
    val date: LocalDate,
    val season: LiturgicalSeason,
    val feastName: String? = null,
    val isSolemnity: Boolean = false,
    val isFeast: Boolean = false,
    val sundayCycle: CycleCalculator.SundayCycle,
    val weekdayCycle: CycleCalculator.WeekdayCycle,
    val feastKey: String? = null,
    val colorCode: String? = null,
    val lectionaryKey: String? = null
) {

}

