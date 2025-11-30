package mivs.liturgicalcalendar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.DatabaseSeeder
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.ui.common.LiturgicalToEventMapper
import java.time.LocalDate

class CalendarRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.dayDao()

    // Tę funkcję wywołujemy przy starcie aplikacji (w ViewModelu)
    suspend fun initializeData() = withContext(Dispatchers.IO) {
        val seeder = DatabaseSeeder(dao)
        seeder.seedDatabase()
    }

    // Klasa modelu czytań
    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String
    )

    // Pobieramy czytania z BAZY DANYCH
    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        val entity = dao.getDay(dayInfo.date)

        if (entity != null) {
            return@withContext DayReadings(
                gospelSigla = entity.gospelSigla,
                psalmResponse = entity.psalmResponse
            )
        } else {
            return@withContext DayReadings("Brak danych w bazie", "Brak danych")
        }
    }

    // Pobieramy dni do kalendarza (Ikony)
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for(i in 0 until len) {
            val date = start.plusDays(i.toLong())

            // 1. Algorytm liczy strukturę (Adwent, Niedziela itp.)
            var day = LiturgicalCalendarCalc.generateDay(date)

            // 2. (OPCJONALNIE) Nadpisujemy nazwę święta nazwą z bazy danych
            // Dzięki temu będziesz miał dokładne nazwy ze starej apki, np. "Św. Jana Bosko"
            val entity = dao.getDay(date)
            if (entity != null) {
                day = day.copy(feastName = entity.feastName)
            }

            days.add(day)
        }
        return@withContext days
    }
}