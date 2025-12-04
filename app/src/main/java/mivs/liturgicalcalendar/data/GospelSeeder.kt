package mivs.liturgicalcalendar.data

import android.util.Log
import kotlinx.coroutines.delay
import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.GospelDao
import mivs.liturgicalcalendar.data.entity.GospelEntity
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GospelSeeder(
    private val dayDao: DayDao,
    private val gospelDao: GospelDao
) {

    suspend fun scrapeAll() {
        Log.d("GospelScraper", "START: Wielkie pobieranie bazy...")

        // 1. ROK 2025 (Legacy)
        var date = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 12, 31)
        while (!date.isAfter(endDate)) {
            val dayInfo = dayDao.getDay(date)
            if (dayInfo != null) downloadIfNeeded(dayInfo.gospelSigla)
            date = date.plusDays(1)
        }

        // 2. ŚWIĘTA RUCHOME (Z pliku MovableFeastsData)
        Log.d("GospelScraper", "--- Pobieranie Świąt Ruchomych ---")
        for (movable in MovableFeastsData.list) {
            downloadIfNeeded(movable.gospelSigla)
        }

        // 3. ŚWIĘTA STAŁE (Z pliku FixedFeastsData - np. św. Pelczar)
        Log.d("GospelScraper", "--- Pobieranie Świąt Stałych ---")
        for (fixed in FixedFeastsData.list) {
            downloadIfNeeded(fixed.gospelSigla)
        }

        // 4. CYKLE A i B (Z pliku LectionaryMap - jeśli go masz)
        // Jeśli nie masz tego pliku, zakomentuj tę pętlę!
        if (true) { // Zmień na false jeśli nie masz LectionaryMap
            Log.d("GospelScraper", "--- Pobieranie z Mapy Lekcjonarza ---")
            for ((_, sigla) in LectionaryMap.map) {
                downloadIfNeeded(sigla)
            }
        }

        Log.d("GospelScraper", "KONIEC: Baza kompletna!")
    }

    private suspend fun downloadIfNeeded(sigla: String) {
        // Dodaliśmy ignorowanie "Z dnia"
        if (sigla.contains("Patrz") || sigla.contains("Z dnia") || sigla.length < 3) return

        val existingText = gospelDao.getGospel(sigla)

        if (existingText.isNullOrEmpty() || existingText.length < 50) {
            val textFromWeb = downloadGospelBySigla(sigla)

            if (textFromWeb.isNotEmpty() && textFromWeb.length > 20) {
                gospelDao.insert(GospelEntity(sigla, textFromWeb))
                Log.d("GospelScraper", "Zapisano [$sigla]")
            } else {
                Log.e("GospelScraper", "PUSTE/BŁĄD dla $sigla")
            }
            delay(500)
        }
    }

    private fun downloadGospelBySigla(rawSigla: String): String {
        return try {
            // 1. Formatowanie URL (spacje na plusy/procenty)
            var siglaClean = rawSigla.replace("–", "-").replace("—", "-").trim()
            siglaClean = siglaClean.replace(" ", "")
            // Wstawiamy spację: "MT5" -> "MT 5"
            siglaClean = siglaClean.replace(Regex("^([A-Za-zŁŚŻŹĆĘÓŃłśżźćęóń]+)(\\d+)"), "$1 $2")
            // Usuwamy litery przy wersetach (np. 12A -> 12)
            siglaClean = siglaClean.replace(Regex("(?<=\\d)[a-zA-Z]"), "")

            val encodedSigla = java.net.URLEncoder.encode(siglaClean, "UTF-8")
            val url = "https://wbiblii.pl/szukaj/$encodedSigla"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .timeout(30000)
                .get()

            // --- STRATEGIA CHIRURGICZNA (Na podstawie Twojego HTML) ---

            val versesDiv = doc.select("div.verses").first()

            if (versesDiv != null) {
                // 1. USUWANIE ŚMIECI (To jest kluczowe!)
                versesDiv.select(".titles").remove()      // Usuwa nagłówki ("DZIAŁALNOŚĆ JEZUSA...")
                versesDiv.select("#footnotes").remove()   // Usuwa przypisy dolne

                // TU BYŁ PROBLEM: Usuwamy kontenery z nawigacją i przyciskami
                versesDiv.select(".extra").remove()       // <--- TO USUWA STRZAŁKI I 'ODSŁUCHAJ'
                versesDiv.select(".pager").remove()       // Usuwa paginację (strzałki)

                // 2. Czyszczenie tekstu
                versesDiv.select("sup").remove()          // Usuwa małe numerki wersetów
                versesDiv.select("strong.cap").remove()   // Usuwa duży numer rozdziału

                // 3. Pobieranie czystego tekstu
                // replace usuwa podwójne spacje, które zostają po usunięciu tagów
                return versesDiv.text().replace("\\s+".toRegex(), " ").trim()
            }

            // Fallback (dla innych stron)
            return ""

        } catch (e: Exception) {
            Log.e("GospelScraper", "Błąd sieci dla $rawSigla: ${e.message}")
            ""
        }
    }
}