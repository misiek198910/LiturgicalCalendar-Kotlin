package mivs.liturgicalcalendar.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.data.LectionaryMap
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.domain.model.LiturgicalSeason
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

    // --- FUNKCJE POMOCNICZE (Agresywne wyszukiwanie) ---
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

    private suspend fun findSmartPsalmText(sigla: String): String? {
        val variants = mutableListOf<String>()
        variants.add(sigla)

        val normalizedDash = sigla.replace("–", "-").replace("—", "-")
        variants.add(normalizedDash)

        if (normalizedDash.startsWith("Ps ")) {
            variants.add(normalizedDash.replaceFirst("Ps ", "Ps"))
        }

        val noWhitespace = normalizedDash.replace("\\s".toRegex(), "")
        variants.add(noWhitespace)

        variants.add(noWhitespace.replace(".", "").replace(",", ""))

        for (variant in variants) {
            val text = psalmDao.getPsalm(variant)
            if (text != null) return text
        }
        return null
    }

    // --- LOGIKA POBIERANIA CZYTAŃ ---
    suspend fun getReadingsForDay(dayInfo: LiturgicalDay): DayReadings = withContext(Dispatchers.IO) {
        println("DEBUG_REPO: ==================================================")
        println("DEBUG_REPO: ANALIZA DNIA: ${dayInfo.date}")
        println("DEBUG_REPO: SEZON: ${dayInfo.season} | CYKL Niedz: ${dayInfo.sundayCycle} | CYKL Dni: ${dayInfo.weekdayCycle}") // NOWA LINIA
        println("DEBUG_REPO: Klucze wejściowe -> LectionaryKey: '${dayInfo.lectionaryKey}' | FeastKey: '${dayInfo.feastKey}'")

        var sigla = "Patrz lekcjonarz"
        var psalmDisplay: String? = "Brak danych"
        var psalmLookupSigla: String? = null
        var psalmFullContent: String? = null
        var feastName: String? = null
        var isPsalmFoundInFixed = false

        // A) Święta Ruchome (wyższy priorytet)
        val isFeriaKey = dayInfo.feastKey?.contains("_W") == true || dayInfo.feastKey?.startsWith("CHRISTMAS_JAN_") == true

        if (dayInfo.feastKey != null && !isFeriaKey && dayInfo.feastKey != "EPIPHANY" && dayInfo.feastKey != "CHRISTMAS_EVE") {
            val movable = movableDao.getFeast(dayInfo.feastKey)
            if (movable != null) {
                println("DEBUG_REPO: [A] Znaleziono Święto Ruchome w bazie: ${dayInfo.feastKey}")
                sigla = movable.gospelSigla
                println("DEBUG_REPO: [A] Przypisano Ewangelię z movable: $sigla")

                // *** POPRAWKA: Pobieranie treści psalmu dla świąt ruchomych ***
                val mPsalmSigla = movable.psalmSigla
                if (!mPsalmSigla.isNullOrBlank() && mPsalmSigla != "Z dnia") {
                    psalmLookupSigla = mPsalmSigla
                    val fullText = findSmartPsalmText(mPsalmSigla)

                    if (!fullText.isNullOrEmpty()) {
                        psalmFullContent = fullText
                        psalmDisplay = "Psalm: $mPsalmSigla, Ref: ${movable.psalmResponse}"
                        isPsalmFoundInFixed = true
                        println("DEBUG_REPO: [A] ✅ Znaleziono treść psalmu w bazie: $mPsalmSigla")
                    } else {
                        psalmDisplay = "Psalm: $mPsalmSigla (BRAK W BAZIE)"
                        psalmFullContent = "⚠️ BRAK PSALMU: $mPsalmSigla"
                        // Jeśli brak w bazie, nie ustawiamy flagi na true, żeby spróbował poszukać w mapie (fallback)
                        // Ale dla świąt ruchomych mapa rzadko ma dane, więc to raczej informacja o błędzie.
                        isPsalmFoundInFixed = true
                        println("DEBUG_REPO: [A] ❌ BRAK treści psalmu w bazie dla: $mPsalmSigla")
                    }
                } else {
                    // Jeśli nie ma sigli, wyświetl sam refren (stara logika)
                    psalmDisplay = "Psalm: ${movable.psalmResponse}"
                    psalmFullContent = "Refren: ${movable.psalmResponse}"
                    isPsalmFoundInFixed = true
                    println("DEBUG_REPO: [A] Brak sigli psalmu w movable, używam tylko refrenu.")
                }
            } else {
                println("DEBUG_REPO: [A] Mimo feastKey=${dayInfo.feastKey}, nie znaleziono wpisu w tabeli movable_feasts!")
            }
        }

        // B) Święta Stałe (z bazy)
        val fixedFeast = fixedDao.getFeast(dayInfo.date.monthValue, dayInfo.date.dayOfMonth)

        if (fixedFeast != null && dayInfo.feastName == fixedFeast.feastName) {
            println("DEBUG_REPO: [B] Znaleziono Święto Stałe: ${fixedFeast.feastName}")

            if (fixedFeast.gospelSigla != "Z dnia") {
                sigla = fixedFeast.gospelSigla
                println("DEBUG_REPO: [B] Nadpisano Ewangelię z fixed: $sigla")
            }

            val specificPsalmSigla = fixedFeast.psalmSigla
            if (specificPsalmSigla != null && specificPsalmSigla != "Z dnia" && specificPsalmSigla.isNotBlank()) {
                val fullText = findSmartPsalmText(specificPsalmSigla)
                if (!fullText.isNullOrEmpty()) {
                    psalmFullContent = fullText
                    psalmDisplay = "Psalm: $specificPsalmSigla, Ref: ${fixedFeast.psalmResponse}"
                    psalmLookupSigla = specificPsalmSigla
                    isPsalmFoundInFixed = true
                    println("DEBUG_REPO: [B] ✅ Znaleziono treść psalmu fixed: $specificPsalmSigla")
                } else {
                    psalmDisplay = "Psalm: $specificPsalmSigla (BRAK W BAZIE)"
                    psalmFullContent = "⚠️ BRAK PSALMU: $specificPsalmSigla"
                    isPsalmFoundInFixed = true
                    println("DEBUG_REPO: [B] ❌ BRAK treści psalmu fixed w bazie: $specificPsalmSigla")
                }
            } else if (fixedFeast.psalmResponse != "Z dnia") {
                psalmDisplay = "Psalm: ${fixedFeast.psalmResponse}"
                psalmFullContent = "Refren: ${fixedFeast.psalmResponse}"
            }
            feastName = fixedFeast.feastName
        }

        // C) Mapa Lekcjonarza (Fallback)
        // Wchodzimy tutaj zawsze, gdy mamy klucz lekcjonarza.
        // W środku decydujemy, czy potrzebujemy Ewangelii (bo jest "Z dnia"), czy Psalmu (bo nie znaleziono w Fixed).
        if (dayInfo.lectionaryKey != null) {

            // 1. Uzupełnienie Ewangelii z Mapy (jeśli w świętach stałych było "Z dnia" lub "Patrz lekcjonarz")
            if (sigla == "Patrz lekcjonarz" || sigla == "Z dnia") {
                val mappedGospel = LectionaryMap.getSigla(dayInfo.lectionaryKey)
                if (mappedGospel != null) println("DEBUG_REPO: [C] ✅ Mapa zwróciła Ewangelię: $mappedGospel")
                if (mappedGospel != null) sigla = mappedGospel
            }

            // 2. Uzupełnienie Psalmu z Mapy (TYLKO jeśli nie znaleziono go wcześniej w Świętach Stałych/Ruchomych)
            if (!isPsalmFoundInFixed) {
                val mappedPsalmData = LectionaryMap.getPsalmData(dayInfo.lectionaryKey)
                if (mappedPsalmData != null) {
                    // ... (tutaj zostaje Twoja stara logika pobierania psalmu) ...
                    val (rawSigla, rawRefrain) = mappedPsalmData
                    println("DEBUG_REPO: [C] Mapa zwróciła dane Psalmu: Sigla=$rawSigla")

                    val finalRefrain = if (psalmDisplay != "Brak danych" && psalmDisplay?.contains("Ref") == true) {
                        psalmDisplay!!.substringAfter("Ref: ").trim()
                    } else rawRefrain

                    psalmLookupSigla = rawSigla
                    val fullContentFromDb = findSmartPsalmText(rawSigla)

                    if (!fullContentFromDb.isNullOrEmpty()) {
                        psalmFullContent = fullContentFromDb
                        psalmDisplay = "Psalm: $rawSigla, Ref: $finalRefrain"
                        println("DEBUG_REPO: [C] ✅ Znaleziono treść psalmu w bazie.")
                    } else {
                        psalmDisplay = "Psalm: $rawSigla (BRAK W BAZIE)"
                        psalmFullContent = "⚠️ BRAK PSALMU: $rawSigla"
                        println("DEBUG_REPO: [C] ❌ BRAK treści psalmu w bazie dla: $rawSigla")
                    }
                }
            }
        }

        println("DEBUG_REPO: [FINAL] Szukam pełnego tekstu Ewangelii dla sigli: '$sigla'")
        var gospelFullText = findSmartGospelText(sigla)

        if (gospelFullText == null && sigla != "Patrz lekcjonarz") {
            gospelFullText = "⚠️ BRAK EWANGELII: $sigla"
            println("DEBUG_REPO: [FINAL] ❌ BŁĄD KRYTYCZNY: Nie znaleziono tekstu w tabeli 'gospel_texts' dla '$sigla'")
        } else if (gospelFullText != null) {
            println("DEBUG_REPO: [FINAL] ✅ SUKCES: Tekst ewangelii znaleziony.")
        }

        println("DEBUG_REPO: ==================================================")

        return@withContext DayReadings(
            gospelSigla = sigla,
            psalmResponse = psalmDisplay ?: "Brak danych",
            psalmSigla = psalmLookupSigla,
            psalmFullText = psalmFullContent,
            dbFeastName = feastName,
            gospelFullText = gospelFullText
        )
    }

    // --- METODA POMOCNICZA: CZY NADPISYWAĆ DZIEŃ? ---
    private fun shouldOverwrite(day: LiturgicalDay, fixedRank: Int): Boolean {
        // 1. Ochrona Świąt Ruchomych i kluczowych dni
        if (day.feastKey != null) {
            val isOverwritableKey = day.feastKey == "EPIPHANY" ||
                    day.feastKey == "CHRISTMAS_EVE"
            // Wielki Piątek, Popielec, Wielkanoc itp. są chronione
            if (!isOverwritableKey) return false
        }

        // 2. OCHRONA OKRESÓW SPECJALNYCH (ZASADY LITURGICZNE)

        // A) Wielki Tydzień i Triduum (Absolutny zakaz nadpisywania przez świętych)
        // Sprawdzamy, czy to 6. tydzień Wielkiego Postu lub Triduum
        if (day.season == LiturgicalSeason.TRIDUUM) return false
        if (day.season == LiturgicalSeason.LENT && day.lectionaryKey?.contains("LENT_W6") == true) return false
        if (day.season == LiturgicalSeason.LENT && day.lectionaryKey?.contains("HOLY_WEEK") == true) return false

        // B) Oktawa Wielkanocy (Absolutny zakaz)
        // Dni oktawy mają klucz EASTER_W1_...
        if (day.season == LiturgicalSeason.EASTER && day.lectionaryKey?.contains("EASTER_W1") == true) return false

        // C) Wielki Post (zwykły) - nadpisujemy tylko Święta (3) i Uroczystości (4)
        // Wspomnienia (1) i (2) są "dowolne" lub znoszone w Poście.
        if (day.season == LiturgicalSeason.LENT) {
            return fixedRank >= 3
        }

        // D) Niedziele Adwentu, Postu i Wielkanocy (Zawsze chronione)
        if (day.lectionaryKey?.contains("SUN") == true) {
            if (day.lectionaryKey.contains("ADVENT") ||
                day.lectionaryKey.contains("LENT") ||
                day.lectionaryKey.contains("EASTER")) {
                return false
            }
        }

        // E) Okres Bożego Narodzenia (Pozwalamy na nadpisywanie w oktawie i po niej)
        if (day.season == LiturgicalSeason.CHRISTMAS) {
            return true
        }

        // Domyślnie (Okres Zwykły): Nadpisz, jeśli to wspomnienie (Rank >= 1)
        return fixedRank >= 1
    }

    // --- POBIERANIE POJEDYNCZEGO DNIA ---
    suspend fun getDay(date: LocalDate): LiturgicalDay = withContext(Dispatchers.IO) {
        var day = LiturgicalCalendarCalc.generateDay(date)
        if (day.feastKey == "CHRIST_KING") day = day.copy(colorCode = "w")

        val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
        if (fixedFeast != null && shouldOverwrite(day, fixedFeast.rank)) {
            val keyForFeast = if (fixedFeast.psalmSigla == "Z dnia" || fixedFeast.gospelSigla == "Z dnia") {
                day.lectionaryKey
            } else {
                fixedFeast.gospelSigla
            }

            day = day.copy(
                feastName = fixedFeast.feastName,
                lectionaryKey = keyForFeast,
                colorCode = fixedFeast.color,
                feastKey = fixedFeast.gospelSigla
            )
        }
        return@withContext day
    }

    // --- POBIERANIE DNI DLA MIESIĄCA ---
    suspend fun getDaysForMonth(year: Int, month: Int): List<LiturgicalDay> = withContext(Dispatchers.IO) {
        val days = mutableListOf<LiturgicalDay>()
        val start = LocalDate.of(year, month, 1)
        val len = start.lengthOfMonth()

        for (i in 0 until len) {
            val date = start.plusDays(i.toLong())
            var day = LiturgicalCalendarCalc.generateDay(date)

            if (day.feastKey == "CHRIST_KING") day = day.copy(colorCode = "w")

            val fixedFeast = fixedDao.getFeast(date.monthValue, date.dayOfMonth)
            if (fixedFeast != null && shouldOverwrite(day, fixedFeast.rank)) {
                val keyForFeast = if (fixedFeast.psalmSigla == "Z dnia" || fixedFeast.gospelSigla == "Z dnia") {
                    day.lectionaryKey
                } else {
                    fixedFeast.gospelSigla
                }

                day = day.copy(
                    feastName = fixedFeast.feastName,
                    lectionaryKey = keyForFeast,
                    colorCode = fixedFeast.color,
                    feastKey = fixedFeast.gospelSigla
                )
            }
            days.add(day)
        }
        days
    }
}