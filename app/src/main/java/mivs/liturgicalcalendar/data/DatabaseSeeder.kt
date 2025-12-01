package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.FixedFeastDao
import mivs.liturgicalcalendar.data.dao.MovableFeastDao // Dodany import
import mivs.liturgicalcalendar.data.entity.DayEntity
import java.time.LocalDate

class DatabaseSeeder(
    private val dayDao: DayDao,
    private val fixedFeastDao: FixedFeastDao,
    private val movableFeastDao: MovableFeastDao // Dodany parametr
) {

    suspend fun seedDatabase() {
        seedLegacyData()  // Tabela 2025
        seedFixedFeasts() // Tabela stała (wieczna)
        seedMovableFeasts() // Tabela ruchoma (wieczna)
    }

    private suspend fun seedLegacyData() {
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

    private suspend fun seedFixedFeasts() {
        if (fixedFeastDao.getCount() > 0) return
        val fixedList = FixedFeastsData.list
        fixedFeastDao.insertAll(fixedList)
    }

    private suspend fun seedMovableFeasts() {
        if (movableFeastDao.getCount() > 0) return
        val movableList = MovableFeastsData.list
        movableFeastDao.insertAll(movableList)
    }
}