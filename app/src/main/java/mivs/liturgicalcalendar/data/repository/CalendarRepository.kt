package mivs.liturgicalcalendar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.DatabaseSeeder
import mivs.liturgicalcalendar.data.GospelSeeder
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import java.time.LocalDate

class CalendarRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.dayDao()
    private val fixedDao = db.fixedFeastDao()
    private val movableDao = db.movableFeastDao()
    private val gospelDao = db.gospelDao()

    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val dbFeastName: String? = null,
        // --- DODANE POLE: ---
        val gospelFullText: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {

        // Zmienne na start
        var sigla = "Patrz lekcjonarz"
        var psalm = "Brak danych w bazie"
        var feastName: String? = null

        // 1. Legacy 2025
        val entity2025 = dao.getDay(dayInfo.date)
        if (entity2025 != null) {
            sigla = entity2025.gospelSigla
            psalm = entity2025.psalmResponse
            feastName = entity2025.feastName
        }
        // 2. Ruchome
        else if (dayInfo.feastKey != null) {
            val movable = movableDao.getFeast(dayInfo.feastKey)
            if (movable != null) {
                sigla = movable.gospelSigla
                psalm = movable.psalmResponse
                // feastName null, bo z algorytmu
            }
        }
        // 3. Stałe
        else {
            val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)
            if (fixedFeast != null) {
                sigla = fixedFeast.gospelSigla
                psalm = fixedFeast.psalmResponse
                feastName = fixedFeast.feastName
            }
        }

        // --- POBIERANIE TEKSTU EWANGELII ---
        val fullText = gospelDao.getGospel(sigla)
        // ----------------------------------

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalm,
            dbFeastName = feastName,
            gospelFullText = fullText // Przekazujemy tekst
        )
    }

    suspend fun initializeData() = withContext(Dispatchers.IO) {
        val seeder = DatabaseSeeder(dao, fixedDao, movableDao)
        seeder.seedDatabase()
    }

    suspend fun runScraper() = withContext(Dispatchers.IO) {
        val scraper = GospelSeeder(db.dayDao(), gospelDao)
        scraper.scrapeAll2025()
    }

    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())
            var day = LiturgicalCalendarCalc.generateDay(date)

            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
            if (fixedFeast != null) {
                if (day.feastName == null || fixedFeast.rank >= 3) {
                    day = day.copy(
                        feastName = fixedFeast.feastName,
                        colorCode = fixedFeast.color
                    )
                }
            }

            val legacyEntity = dao.getDay(date)
            if (legacyEntity != null) {
                day = day.copy(
                    feastName = legacyEntity.feastName,
                    colorCode = legacyEntity.colorCode
                )
            }
            days.add(day)
        }
        return@withContext days
    }
}