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

    data class DayReadings(
        val gospelSigla: String,
        val psalmResponse: String,
        val psalmSigla: String? = null,
        val psalmFullText: String? = null,
        val dbFeastName: String? = null,
        val gospelFullText: String? = null
    )

    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        var sigla = "Patrz lekcjonarz"
        var psalmDisplay: String? = "Brak danych"
        var psalmLookupSigla: String? = null
        var psalmFullContent: String? = null
        var feastName: String? = null

        // Zmienna pomocnicza
        var isPsalmFoundInFixed = false

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

                    // 1. Ewangelia
                    if (fixedFeast.gospelSigla != "Z dnia") sigla = fixedFeast.gospelSigla

                    // 2. Psalm
                    val specificPsalmSigla = fixedFeast.psalmSigla

                    if (specificPsalmSigla != null && specificPsalmSigla != "Z dnia" && specificPsalmSigla.isNotBlank()) {
                        val fullText = findSmartPsalmText(specificPsalmSigla)

                        if (!fullText.isNullOrEmpty()) {
                            psalmFullContent = fullText
                            psalmDisplay = "Psalm: $specificPsalmSigla, Ref: ${fixedFeast.psalmResponse}"
                            psalmLookupSigla = specificPsalmSigla
                            isPsalmFoundInFixed = true
                        } else {
                            // --- DEBUG MODE: BRAK TEKSTU W BAZIE ---
                            psalmDisplay = "Psalm: $specificPsalmSigla (BRAK W BAZIE)"
                            psalmFullContent = "⚠️ BRAK PSALMU W BAZIE DLA: $specificPsalmSigla\nRefren: ${fixedFeast.psalmResponse}"
                            isPsalmFoundInFixed = true
                        }
                    } else if (fixedFeast.psalmResponse != "Z dnia") {
                        psalmDisplay = "Psalm: ${fixedFeast.psalmResponse}"
                        psalmFullContent = "Refren: ${fixedFeast.psalmResponse}"
                    }

                    feastName = fixedFeast.feastName
                }
            }
        }

        // --- SEKCJA 3: UŻYCIE MAPY LEKCJONARZA ---

        if (dayInfo.lectionaryKey != null) {

            // Ewangelia
            if (sigla == "Patrz lekcjonarz" || sigla == "Z dnia") {
                val mappedGospel = LectionaryMap.getSigla(dayInfo.lectionaryKey)
                if (mappedGospel != null) sigla = mappedGospel
            }

            // Psalm (jeśli nie znaleziono w święcie stałym)
            if (!isPsalmFoundInFixed) {
                val mappedPsalmData = LectionaryMap.getPsalmData(dayInfo.lectionaryKey)
                if (mappedPsalmData != null) {
                    val (rawSigla, rawRefrain) = mappedPsalmData

                    val finalRefrain = if (psalmDisplay != "Brak danych" && psalmDisplay?.contains("Refren") == true) {
                        rawRefrain
                    } else rawRefrain

                    psalmLookupSigla = rawSigla

                    val fullContentFromDb = findSmartPsalmText(rawSigla)

                    if (!fullContentFromDb.isNullOrEmpty()) {
                        psalmFullContent = fullContentFromDb
                        psalmDisplay = "Psalm: $rawSigla, Ref: $finalRefrain"
                    } else {
                        // --- DEBUG MODE: BRAK TEKSTU W BAZIE ---
                        psalmDisplay = "Psalm: $rawSigla (BRAK W BAZIE)"
                        psalmFullContent = "⚠️ BRAK PSALMU W BAZIE DLA: $rawSigla\nSprawdź czy w tabeli 'psalm_texts' istnieje dokładnie taki klucz (lub bez spacji)."
                    }
                }
            }
        }

        // 4. INTELIGENTNE POBIERANIE TREŚCI EWANGELII
        var gospelFullText = findSmartGospelText(sigla)

        // --- DEBUG MODE: BRAK EWANGELII ---
        if (gospelFullText == null && sigla != "Patrz lekcjonarz" && sigla != "Z dnia") {
            gospelFullText = "⚠️ BRAK EWANGELII W BAZIE DLA: $sigla\n\nSprawdź tabelę 'gospel_texts'. Aplikacja szukała wariantów:\n1. $sigla\n2. ${sigla.replace(" ", "")}\n3. ${sigla.replace("–", "-")}"
        }

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalmDisplay ?: "Brak danych",
            psalmSigla = psalmLookupSigla,
            psalmFullText = psalmFullContent,
            dbFeastName = feastName,
            gospelFullText = gospelFullText
        )
    }

    // Funkcja dla Ewangelii
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

    // Funkcja dla Psalmów
    private suspend fun findSmartPsalmText(sigla: String): String? {
        val variants = mutableListOf<String>()
        variants.add(sigla)
        variants.add(sigla.replace(" ", ""))
        variants.add(sigla.replace(" ", "").replace("–", "-").replace("—", "-"))
        variants.add(sigla.replace("–", "-"))

        for (variant in variants) {
            val text = psalmDao.getPsalm(variant)
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