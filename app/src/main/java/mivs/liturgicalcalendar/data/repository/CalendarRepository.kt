package mivs.liturgicalcalendar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.DatabaseSeeder
import mivs.liturgicalcalendar.data.GospelSeeder
import mivs.liturgicalcalendar.data.LectionaryMap
import mivs.liturgicalcalendar.data.PsalmSeeder
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

    private val psalmDao = db.psalmDao()

    fun getDayDaoPublic() = dao

    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val dbFeastName: String? = null,
        val gospelFullText: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        // Zmienne na start
        var sigla = "Patrz lekcjonarz"
        var psalm = "Brak danych w bazie"
        var feastName: String? = null

        // 1. Legacy 2025 (Priorytet)
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
            }
        }
        // 3. Stałe (po dacie)
        else {
            val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)
            if (fixedFeast != null) {
                // JEŚLI SIGLA TO "Z dnia", NIE NADPISUJEMY JEJ!
                if (fixedFeast.gospelSigla != "Z dnia") {
                    sigla = fixedFeast.gospelSigla
                }

                // Ale nazwę święta i psalm (jeśli jest własny) bierzemy
                if (fixedFeast.psalmResponse != "Z dnia") {
                    psalm = fixedFeast.psalmResponse
                }
                feastName = fixedFeast.feastName
            }
        }

        // 4. Mapy Lekcjonarza (Dla lat A/B/C)
        if (dayInfo.lectionaryKey != null) {
            // Pobierz Ewangelię
            val mappedGospel = LectionaryMap.getSigla(dayInfo.lectionaryKey)
            if (mappedGospel != null) {
                sigla = mappedGospel
            }

            // Pobierz Psalm
            val mappedPsalm = LectionaryMap.getPsalmData(dayInfo.lectionaryKey)
            if (mappedPsalm != null) {
                // mappedPsalm.first = Sigla, mappedPsalm.second = Refren
                // Składamy to w całość dla użytkownika
                psalm = "${mappedPsalm.first}\nRef: ${mappedPsalm.second}"
            }
        }

        // Pobranie treści
        val lookupSigla = sigla.replace("–", "-").replace("—", "-").trim()
        val fullText = gospelDao.getGospel(lookupSigla) ?: gospelDao.getGospel(sigla)

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalm,
            dbFeastName = feastName,
            gospelFullText = fullText
        )
    }

    // --- FUNKCJE BUDUJĄCE BAZĘ (Tymczasowe) ---

    suspend fun initializeData() = withContext(Dispatchers.IO) {
        // Teraz przekazujemy 3 DAO!
        val seeder = DatabaseSeeder(dao, fixedDao, movableDao)
        seeder.seedDatabase()
    }

    suspend fun runScraper() = withContext(Dispatchers.IO) {
        val scraper = GospelSeeder(dao, gospelDao)
        // Wywołujemy scrapeAll (jeśli tak nazwałeś funkcję w GospelSeeder)
        // Jeśli w GospelSeeder masz 'scrapeAll2025', zmień tu nazwę na taką samą!
        scraper.scrapeAll()
    }

    suspend fun runPsalmScraper() = withContext(Dispatchers.IO) {
        val scraper = PsalmSeeder(psalmDao)
        scraper.scrapeAll()
    }

    // ------------------------------------------

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