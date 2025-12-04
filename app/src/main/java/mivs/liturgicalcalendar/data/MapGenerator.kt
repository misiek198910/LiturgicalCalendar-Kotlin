package mivs.liturgicalcalendar.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import java.time.LocalDate

class MapGenerator(private val dayDao: DayDao) {

    suspend fun generateOrdinaryTime() = withContext(Dispatchers.IO) {
        Log.d("MapGen", "// --- DNI POWSZEDNIE OKRESU ZWYKŁEGO ---")

        var date = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 12, 31)

        while (!date.isAfter(endDate)) {
            // 1. Obliczamy klucz
            val dayInfo = LiturgicalCalendarCalc.generateDay(date)
            val key = dayInfo.lectionaryKey

            // 2. Filtrujemy tylko Okres Zwykły (ORD_W...)
            // Ignorujemy niedziele (bo już je masz) i inne okresy
            if (key != null && key.startsWith("ORD_W")) {
                // Pobieramy siglę z bazy 2025 (Legacy)
                val entity = dayDao.getDay(date)
                if (entity != null) {
                    Log.d("MapGen", "\"$key\" to \"${entity.gospelSigla}\",")
                }
            }
            date = date.plusDays(1)
        }
        Log.d("MapGen", "// KONIEC ODKRYWANIA")
    }
}