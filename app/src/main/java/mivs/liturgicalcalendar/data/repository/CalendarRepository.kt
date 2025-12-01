package mivs.liturgicalcalendar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.DatabaseSeeder
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import java.time.LocalDate

class CalendarRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.dayDao()
    private val fixedDao = db.fixedFeastDao()
    private val movableDao = db.movableFeastDao() // Dodane DAO

    suspend fun initializeData() = withContext(Dispatchers.IO) {
        // Przekazujemy wszystkie 3 DAO do Seedera
        val seeder = DatabaseSeeder(dao, fixedDao, movableDao)
        seeder.seedDatabase()
    }

    // --- CZYTANIA ---
    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val dbFeastName: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        // 1. Priorytet: Legacy 2025 (Najdokładniejsze dla tego roku)
        val entity2025 = dao.getDay(dayInfo.date)
        if (entity2025 != null) {
            return@withContext DayReadings(
                gospelSigla = entity2025.gospelSigla,
                psalmResponse = entity2025.psalmResponse,
                dbFeastName = entity2025.feastName
            )
        }

        // 2. Jeśli to rok 2026+: Sprawdzamy Święta RUCHOME (po kluczu)
        if (dayInfo.feastKey != null) {
            val movable = movableDao.getFeast(dayInfo.feastKey)
            if (movable != null) {
                return@withContext DayReadings(
                    gospelSigla = movable.gospelSigla,
                    psalmResponse = movable.psalmResponse,
                    // Nazwę bierzemy z algorytmu, bo tabela movable jej nie ma (trzyma tylko klucz i teksty)
                    dbFeastName = null
                )
            }
        }

        // 3. Jeśli to rok 2026+: Sprawdzamy Święta STAŁE (po dacie)
        val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)
        if (fixedFeast != null) {
            return@withContext DayReadings(
                gospelSigla = fixedFeast.gospelSigla,
                psalmResponse = fixedFeast.psalmResponse,
                dbFeastName = fixedFeast.feastName
            )
        }

        // 4. Fallback
        return@withContext DayReadings("Patrz lekcjonarz", "Brak danych w bazie")
    }

    // --- KALENDARZ (Ikony) ---
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())

            // 1. Algorytm (tu teraz generowane są też klucze feastKey)
            var day = LiturgicalCalendarCalc.generateDay(date)

            // 2. Nakładka: Święta Stałe (tylko dla nazwy w widoku miesiąca dla lat 2026+)
            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
            if (fixedFeast != null) {
                if (day.feastName == null || fixedFeast.rank >= 3) {
                    day = day.copy(feastName = fixedFeast.feastName)
                }
            }

            // 3. Nakładka: Legacy 2025 (Dla pewności w tym roku)
            val legacyEntity = dao.getDay(date)
            if (legacyEntity != null) {
                day = day.copy(feastName = legacyEntity.feastName)
            }

            days.add(day)
        }
        return@withContext days
    }
}