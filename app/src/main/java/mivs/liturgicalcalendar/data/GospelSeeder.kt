package mivs.liturgicalcalendar.data

import android.util.Log
import kotlinx.coroutines.delay
import mivs.liturgicalcalendar.data.dao.DayDao
import mivs.liturgicalcalendar.data.dao.GospelDao
import mivs.liturgicalcalendar.data.entity.GospelEntity
import org.jsoup.Jsoup
import java.time.LocalDate

class GospelSeeder(
    private val dayDao: DayDao,
    private val gospelDao: GospelDao
) {

    suspend fun scrapeAll2025() {
        Log.d("GospelScraper", "START: Pobieranie wg SIGLI (wbiblii.pl)...")

        var date = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 12, 31)

        while (!date.isAfter(endDate)) {
            val dayInfo = dayDao.getDay(date)

            if (dayInfo != null) {
                // Pobieramy sigla z bazy (np. "MT1,1-17")
                val sigla = dayInfo.gospelSigla

                // Sprawdzamy czy już mamy tekst (żeby nie męczyć serwera)
                val existingText = gospelDao.getGospel(sigla)

                if (existingText == null) {
                    val textFromWeb = downloadGospelBySigla(sigla)

                    if (textFromWeb.isNotEmpty() && textFromWeb.length > 20) {
                        gospelDao.insert(GospelEntity(sigla, textFromWeb))
                        Log.d("GospelScraper", "Zapisano [$sigla]: ${textFromWeb.take(30)}...")
                    } else {
                        Log.e("GospelScraper", "PUSTE dla $sigla (Data: $date)")
                    }

                    // Czekamy chwilę, to kulturalne przy scrapowaniu
                    delay(300)
                } else {
                    // Log.d("GospelScraper", "Pominięto (już jest): $sigla")
                }
            }
            date = date.plusDays(1)
        }
        Log.d("GospelScraper", "KONIEC: Wszystkie ewangelie pobrane!")
    }

    private fun downloadGospelBySigla(rawSigla: String): String {
        return try {
            val cleanSigla = rawSigla.replace("–", "-").replace(" ", "")
            val url = "https://wbiblii.pl/szukaj/$cleanSigla"

            val doc = Jsoup.connect(url).userAgent("...").timeout(30000).get()

            // 1. REMOVE GARBAGE NODES
            doc.select(".audio-player, .btn, .navigation, .nav-links, .footer, #footer").remove()
            doc.select("div.titles").remove() // Remove section titles if unwanted
            doc.select("div.footnotes").remove()
            doc.select("sup, strong.cap").remove() // Remove verse numbers

            // 2. EXTRACT TEXT
            // Try specific container first
            var text = doc.select("div.verses").text()

            if (text.isEmpty()) {
                // Fallback to paragraphs, excluding navigation
                text = doc.select("p").filter {
                    !it.text().contains("Dodaj do ulubionych") && !it.text().contains("Odsłuchaj")
                }.joinToString("\n") { it.text() }
            }

            // 3. AGGRESSIVE TEXT CLEANING (String manipulation)
            // Cut off at "Odsłuchaj"
            if (text.contains("Odsłuchaj")) {
                text = text.substringBefore("Odsłuchaj")
            }
            // Cut off at "Przypisy"
            if (text.contains("Przypisy")) {
                text = text.substringBefore("Przypisy")
            }
            // Cut off at arrows (navigation)
            val arrows = listOf("←", "→", "<", ">")
            for (arrow in arrows) {
                if (text.contains(arrow)) {
                    // Check if it's at the end (likely navigation)
                    val index = text.lastIndexOf(arrow)
                    if (index > text.length - 50) { // Only cut if it's near the end
                        text = text.substring(0, index)
                    }
                }
            }

            return text.trim()

        } catch (e: Exception) {
            Log.e("GospelScraper", "Error: ${e.message}")
            ""
        }
    }
}