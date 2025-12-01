package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.FixedFeastDao
import mivs.liturgicalcalendar.data.entity.DayEntity
import java.time.LocalDate

// ZMIANA: Dodaliśmy 'fixedFeastDao' do konstruktora
class DatabaseSeeder(
    private val dayDao: DayDao,
    private val fixedFeastDao: FixedFeastDao
) {

    suspend fun seedDatabase() {
        // Uruchamiamy obie procedury importu
        seedLegacyData()  // To wypełni tabelę na rok 2025 (DayEntity)
        seedFixedFeasts() // To wypełni tabelę wieczną (FixedFeastEntity)
    }

    // --- LOGIKA DLA ROKU 2025 (Twoja stara logika) ---
    private suspend fun seedLegacyData() {
        // Jeśli tabela 2025 nie jest pusta, pomijamy
        if (dayDao.getCount() > 0) return

        val events = LegacyData2025.events
        val colors = LegacyData2025.colors
        val gospels = LegacyData2025.gospels
        val psalms = LegacyData2025.psalms

        val size = minOf(events.size, colors.size, gospels.size, psalms.size)
        val daysToInsert = mutableListOf<DayEntity>()
        var currentDate = LocalDate.of(2025, 1, 1)

        for (i in 0 until size) {
            val entity = DayEntity(
                date = currentDate,
                feastName = events[i],
                colorCode = colors[i],
                gospelSigla = gospels[i],
                psalmResponse = psalms[i]
            )
            daysToInsert.add(entity)
            currentDate = currentDate.plusDays(1)
        }

        dayDao.insertAll(daysToInsert)
    }

    // --- LOGIKA DLA TABELI WIECZNEJ (Nowa) ---
    private suspend fun seedFixedFeasts() {
        // Jeśli tabela stała nie jest pusta, pomijamy
        if (fixedFeastDao.getCount() > 0) return

        // Pobieramy listę, którą utworzyliśmy w pliku FixedFeastsData.kt
        val fixedList = FixedFeastsData.list

        fixedFeastDao.insertAll(fixedList)
    }
}