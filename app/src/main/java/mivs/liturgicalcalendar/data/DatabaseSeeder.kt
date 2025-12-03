package mivs.liturgicalcalendar.data

import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.FixedFeastDao
import mivs.liturgicalcalendar.data.dao.MovableFeastDao
import mivs.liturgicalcalendar.data.entity.DayEntity
import mivs.liturgicalcalendar.data.entity.FixedFeastEntity
import java.time.LocalDate

class DatabaseSeeder(
    private val dayDao: DayDao,
    private val fixedFeastDao: FixedFeastDao,
    private val movableFeastDao: MovableFeastDao
) {

    suspend fun seedDatabase() {
        // 1. Ładujemy rok 2025 (Legacy)
        seedLegacyData()

        // 2. Ładujemy święta ruchome (Movable)
        seedMovableFeasts()

        // 3. AUTOMAT: Wyciągamy świętych z 2025 do bazy wiecznej
        migrateSaintsFromLegacy()

        // 4. RĘCZNE: Nadpisujemy/Dodajemy Twoje ręczne poprawki (np. św. Pelczara)
        // To musi być PO migracji, żeby Twoje poprawki miały priorytet
        seedFixedFeasts()
    }

    // --- ROK 2025 ---
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
            daysToInsert.add(DayEntity(currentDate, events[i], colors[i], gospels[i], psalms[i]))
            currentDate = currentDate.plusDays(1)
        }
        dayDao.insertAll(daysToInsert)
    }

    // --- ŚWIĘTA RUCHOME ---
    private suspend fun seedMovableFeasts() {
        // Zawsze nadpisujemy, żeby mieć pewność
        movableFeastDao.insertAll(MovableFeastsData.list)
    }

    // --- ŚWIĘTA STAŁE (RĘCZNE) ---
    private suspend fun seedFixedFeasts() {
        // Zawsze nadpisujemy
        fixedFeastDao.insertAll(FixedFeastsData.list)
    }

    // --- MIGRATOR (Z 2025 DO WIECZNEJ) ---
    private suspend fun migrateSaintsFromLegacy() {
        // Jeśli już mamy dużo danych w fixed, to może pomińmy ten krok,
        // albo puśćmy go, żeby uzupełnić braki.
        // Room z OnConflictStrategy.REPLACE (w DAO) zadba o to, żeby nie było duplikatów.

        val events = LegacyData2025.events
        val colors = LegacyData2025.colors
        val gospels = LegacyData2025.gospels
        val psalms = LegacyData2025.psalms
        val size = minOf(events.size, colors.size, gospels.size, psalms.size)

        val saintsToInsert = mutableListOf<FixedFeastEntity>()
        var currentDate = LocalDate.of(2025, 1, 1)

        for (i in 0 until size) {
            val name = events[i]

            // Jeśli to nie jest dzień zwykły ani święto ruchome -> to jest Święty!
            if (!shouldSkip(name)) {
                val entity = FixedFeastEntity(
                    month = currentDate.monthValue,
                    day = currentDate.dayOfMonth,
                    feastName = name,
                    color = colors[i],
                    rank = 1, // Domyślnie wspomnienie
                    gospelSigla = gospels[i],
                    psalmResponse = psalms[i]
                )
                saintsToInsert.add(entity)
            }
            currentDate = currentDate.plusDays(1)
        }
        fixedFeastDao.insertAll(saintsToInsert)
    }

    // Filtr: Co NIE jest świętem stałym?
    private fun shouldSkip(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("dzień zwykły") ||
                n.contains("niedziela") ||
                n.contains("środa popielcowa") ||
                n.contains("wielki czwartek") || n.contains("wielki piątek") ||
                n.contains("wielka sobota") || n.contains("wielki poniedziałek") ||
                n.contains("wielki wtorek") || n.contains("wielka środa") ||
                n.contains("zmartwychwstania") ||
                n.contains("oktawie") ||
                n.contains("wniebowstąpienie") ||
                n.contains("zesłanie ducha") ||
                n.contains("świętej rodziny") ||
                n.contains("chrzest pański") ||
                n.contains("trójcy") ||
                n.contains("ciała i krwi") ||
                n.contains("serca pana jezusa") ||
                n.contains("matki kościoła") ||
                n.contains("króla wszechświata")
    }
}