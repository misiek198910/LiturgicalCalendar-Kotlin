package mivs.liturgicalcalendar.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.LectionaryMap
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import java.time.LocalDate

class CalendarRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val fixedDao = db.fixedFeastDao()
    private val movableDao = db.movableFeastDao()
    private val gospelDao = db.gospelDao()
    private val psalmDao = db.psalmDao()

    // ZMODYFIKOWANA KLASA Z PEŁNYMI DANYMI PSALMU
    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String, // SKRÓCONA LINIA (Refren + Sigla)
        val psalmSigla: String? = null,
        val psalmFullText: String? = null, // PEŁNA TREŚĆ
        val dbFeastName: String? = null,
        val gospelFullText: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        var sigla = "Patrz lekcjonarz"
        var psalmDisplay: String? = "Brak danych"
        var psalmLookupSigla: String? = null
        var psalmFullContent: String? = null
        var feastName: String? = null

        // 1. Święta Ruchome / 2. Święta Stałe (ustalenie początkowej sigli i refrenu)
        // Jeśli znajdziemy wpis w bazie, używamy go jako punkt startowy

        // --- SEKCJA 1 & 2: USTALENIE NAZWY ŚWIĘTA I PODSTAWOWEJ SYGLI/REFRENU ---

        // A) Święta Ruchome
        if (dayInfo.feastKey != null) {
            val movable = movableDao.getFeast(dayInfo.feastKey)
            if (movable != null) {
                sigla = movable.gospelSigla
                psalmDisplay = "Psalm: ${movable.psalmResponse}"
                psalmFullContent = "Refren: ${movable.psalmResponse}"
            }
        }
        // B) Święta Stałe
        else {
            val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)
            if (fixedFeast != null) {
                if (dayInfo.feastName == null || fixedFeast.rank >= 3) {
                    if (fixedFeast.gospelSigla != "Z dnia") sigla = fixedFeast.gospelSigla
                    if (fixedFeast.psalmResponse != "Z dnia") {
                        psalmDisplay = "Psalm: ${fixedFeast.psalmResponse}"
                        psalmFullContent = "Refren: ${fixedFeast.psalmResponse}"
                    }
                    feastName = fixedFeast.feastName
                }
            }
        }

        // --- SEKCJA 3: UŻYCIE MAPY LEKCJONARZA DO ZNALEZIENIA PEŁNEGO TEKSTU PSALMU ---

        if (dayInfo.lectionaryKey != null && (sigla == "Patrz lekcjonarz" || sigla == "Z dnia")) {
            // Ewangelia (nadpisanie sigli jeśli zależy od klucza)
            val mappedGospel = LectionaryMap.getSigla(dayInfo.lectionaryKey)
            if (mappedGospel != null) sigla = mappedGospel

            // Psalm: pobranie sigli i treści z Mapy/DB
            val mappedPsalmData = LectionaryMap.getPsalmData(dayInfo.lectionaryKey)
            if (mappedPsalmData != null) {
                val (rawSigla, rawRefrain) = mappedPsalmData
                psalmLookupSigla = rawSigla

                // Pobieramy pełną treść psalmu z bazy
                val fullContentFromDb = psalmDao.getPsalm(rawSigla)

                if (!fullContentFromDb.isNullOrEmpty()) {
                    // NOWA WERSJA: Pełna treść to TYLKO tekst Psalmu z bazy (bez powtórzonego Refrenu)
                    psalmFullContent = fullContentFromDb

                    // Skrócony tekst do wyświetlenia na ekranie głównym (Sigla + Refren)
                    psalmDisplay = "Psalm: $rawSigla, Ref: $rawRefrain"
                } else {
                    psalmDisplay = "Psalm: $rawSigla, Ref: $rawRefrain (Brak treści w DB)"
                    psalmFullContent = null
                }
            }
        }

        // 4. INTELIGENTNE POBIERANIE TREŚCI EWANGELII
        val gospelFullText = findSmartGospelText(sigla)

        if (gospelFullText == null) {
            Log.e("LITURGY_DB", "BRAK TEKSTU EWANGELII DLA: '$sigla'")
        }

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalmDisplay ?: "Brak danych", // Skrócona linia do wyświetlenia
            psalmSigla = psalmLookupSigla,
            psalmFullText = psalmFullContent,
            dbFeastName = feastName,
            gospelFullText = gospelFullText
        )
    }

    // --- Próbuje znaleźć tekst ignorując spacje i rodzaje myślników (findSmartGospelText pozostaje bez zmian) ---
    private suspend fun findSmartGospelText(sigla: String): String? {
        val variants = mutableListOf<String>()

        variants.add(sigla)
        variants.add(sigla.replace(" ", ""))
        variants.add(sigla.replace(" ", "").replace("–", "-").replace("—", "-"))
        variants.add(sigla.replace("–", "-"))
        variants.add(sigla.replace("-", "–"))

        for (variant in variants) {
            var text = gospelDao.getGospel(variant)
            if (text != null) return text
            text = gospelDao.getGospel(variant.uppercase())
            if (text != null) return text
        }
        return null
    }
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())
            var day = LiturgicalCalendarCalc.generateDay(date)

            if (day.feastKey == "CHRIST_KING") {
                day = day.copy(colorCode = "w")
            }

            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)

            if (fixedFeast != null) {
                if (day.feastName == null || fixedFeast.rank >= 3) {
                    day = day.copy(
                        feastName = fixedFeast.feastName,
                        colorCode = fixedFeast.color
                    )
                }
            }
            days.add(day)
        }
        return@withContext days
    }
}