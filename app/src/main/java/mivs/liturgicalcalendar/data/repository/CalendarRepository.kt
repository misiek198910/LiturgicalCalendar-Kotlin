package mivs.liturgicalcalendar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.DatabaseSeeder
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
import java.time.LocalDate

class CalendarRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.dayDao()
    private val fixedDao = db.fixedFeastDao() // Nowe DAO

    suspend fun initializeData() = withContext(Dispatchers.IO) {
        // Przekazujemy oba DAO do Seedera
        val seeder = DatabaseSeeder(dao, fixedDao)
        seeder.seedDatabase()
    }

    // --- CZYTANIA ---
    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val dbFeastName: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        // 1. Najpierw szukamy w Legacy (Rok 2025 - pełne dane)
        val entity2025 = dao.getDay(dayInfo.date)
        if (entity2025 != null) {
            return@withContext DayReadings(
                gospelSigla = entity2025.gospelSigla,
                psalmResponse = entity2025.psalmResponse,
                dbFeastName = entity2025.feastName
            )
        }

        // 2. Jeśli nie ma w Legacy (np. rok 2026), sprawdzamy czy to Święto Stałe
        val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)
        if (fixedFeast != null) {
            return@withContext DayReadings(
                gospelSigla = "Patrz lekcjonarz", // Nie mamy czytań na 2026 w bazie
                psalmResponse = "Patrz lekcjonarz",
                dbFeastName = fixedFeast.feastName
            )
        }

        return@withContext DayReadings("Brak danych", "Brak danych")
    }

    // --- KALENDARZ (Ikony) ---
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())

            // 1. Baza: Algorytm (Ruchome)
            var day = LiturgicalCalendarCalc.generateDay(date)

            // 2. Nakładka: Święta Stałe (z nowej tabeli) - dla lat innych niż 2025
            // Jeśli to 2026+, sprawdzamy fixed_feasts
            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
            if (fixedFeast != null) {
                // Prosta logika: Stałe święta nadpisują zwykłe dni
                if (day.feastName == null || fixedFeast.rank >= 3) {
                    day = day.copy(feastName = fixedFeast.feastName)
                    // TODO: Tu w przyszłości dodasz mapowanie koloru ze stringa "w" na Enum Season
                }
            }

            // 3. Nakładka: Legacy 2025 (Najwyższy priorytet dla obecnego roku)
            val legacyEntity = dao.getDay(date)
            if (legacyEntity != null) {
                day = day.copy(feastName = legacyEntity.feastName)
            }

            days.add(day)
        }
        return@withContext days
    }
}