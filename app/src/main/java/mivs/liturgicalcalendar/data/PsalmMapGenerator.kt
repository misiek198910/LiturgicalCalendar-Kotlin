package mivs.liturgicalcalendar.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PsalmMapGenerator {

    suspend fun generatePsalmMap() = withContext(Dispatchers.IO) {
        Log.d("PsalmGen", "// --- MAPA PSALMÓW (Pustynia w Mieście) ---")

        // Skanujemy rok 2026
        var date = LocalDate.of(2026, 1, 1)
        val endDate = LocalDate.of(2026, 12, 31)

        while (!date.isAfter(endDate)) {
            val dayInfo = LiturgicalCalendarCalc.generateDay(date)
            val key = dayInfo.lectionaryKey

            if (key != null) {
                val psalmData = fetchPsalmMetadata(date)

                if (psalmData != null) {
                    // Format: "KLUCZ" to Pair("SIGLA", "REFREN"),
                    Log.d("PsalmGen", "\"$key\" to Pair(\"${psalmData.first}\", \"${psalmData.second}\"),")
                } else {
                    // Log.e("PsalmGen", "// BRAK: $date") // Można odkomentować do debugowania
                }
                delay(300)
            }
            date = date.plusDays(1)
        }
        Log.d("PsalmGen", "// KONIEC GENEROWANIA")
    }

    private fun fetchPsalmMetadata(date: LocalDate): Pair<String, String>? {
        return try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val url = "https://www.pustyniawmiescie.pl/czytania/${date.format(fmt)}"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(20000)
                .get()

            // --- NOWA LOGIKA PARSOWANIA ---

            // 1. Szukamy elementu <a>, którego ID zaczyna się od "psalm_"
            // np. <a id="psalm_0">Ps 67,2-3.5.8</a>
            val psalmAnchor = doc.select("a[id^=psalm_]").first() ?: return null

            // Mamy Siglę!
            val sigla = psalmAnchor.text().trim()

            // 2. Szukamy Refrenu
            // Refren znajduje się w węźle tekstowym zaraz PO paragrafie zawierającym siglę.
            // Struktura: <p><b><a id="psalm_0">...</a></b></p> REFREN: ... <p>

            var refrain = ""

            // Wychodzimy w górę do rodzica <p>, w którym siedzi sigla
            var parentBlock = psalmAnchor.parent()
            while (parentBlock != null && parentBlock.tagName() != "p") {
                parentBlock = parentBlock.parent()
            }

            if (parentBlock != null) {
                // Bierzemy następny węzeł (sibling) po paragrafie <p>
                val nextNode = parentBlock.nextSibling()

                if (nextNode is TextNode) {
                    // To jest nasz tekst "REFREN: Bóg miłosierny..."
                    refrain = nextNode.text()
                } else if (nextNode != null) {
                    // Czasem może być w czymś innym, próbujemy toString()
                    refrain = nextNode.toString()
                }
            }

            // Czyścimy refren ze słowa "REFREN:" i białych znaków
            refrain = refrain.replace("REFREN:", "", ignoreCase = true).trim()

            // Jeśli refren jest pusty, próbujemy fallback (szukamy w całym tekście)
            if (refrain.isEmpty()) {
                val fullText = doc.text()
                if (fullText.contains("REFREN:")) {
                    refrain = fullText.substringAfter("REFREN:").substringBefore("Czytanie").trim()
                }
            }

            if (sigla.isNotEmpty()) {
                return Pair(sigla, refrain)
            }

            return null

        } catch (e: Exception) {
            Log.e("PsalmGen", "Błąd dla $date: ${e.message}")
            null
        }
    }
}