package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.entity.DayEntity
import java.time.LocalDate

class DatabaseSeeder(private val dao: DayDao) {

    suspend fun seedDatabase() {
        // 1. Sprawdzamy, czy baza jest pusta. Jeśli nie, to znaczy że już zaimportowaliśmy.
        val count = dao.getCount()
        if (count > 0) return

        // 2. Pobieramy dane z Twojego pliku LegacyData2025
        val events = LegacyData2025.events
        val colors = LegacyData2025.colors
        val gospels = LegacyData2025.gospels
        val psalms = LegacyData2025.psalms

        // Zabezpieczenie: bierzemy najkrótszą listę, żeby nie wyjść poza zakres
        val size = minOf(events.size, colors.size, gospels.size, psalms.size)

        val daysToInsert = mutableListOf<DayEntity>()
        var currentDate = LocalDate.of(2025, 1, 1) // Twoje dane w strings.xml zaczynają się od 1 Stycznia

        for (i in 0 until size) {
            val entity = DayEntity(
                date = currentDate,
                feastName = events[i],
                colorCode = colors[i],
                gospelSigla = gospels[i],
                psalmResponse = psalms[i]
            )
            daysToInsert.add(entity)

            // Przesuwamy datę o 1 dzień
            currentDate = currentDate.plusDays(1)
        }

        // 3. Zapisujemy wszystko do bazy
        dao.insertAll(daysToInsert)
    }
}