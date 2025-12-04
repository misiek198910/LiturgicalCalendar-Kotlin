package mivs.liturgicalcalendar.data

import android.util.Log
import kotlinx.coroutines.delay
import mivs.liturgicalcalendar.data.dao.PsalmDao
import mivs.liturgicalcalendar.data.entity.PsalmEntity
import org.jsoup.Jsoup
import java.net.URLEncoder

class PsalmSeeder(
    private val psalmDao: PsalmDao
) {

    suspend fun scrapeAll() {
        Log.d("PsalmScraper", "START: Pobieranie tekstów Psalmów z wbiblii.pl...")

        // Iterujemy po mapie psalmów z LectionaryMap
        for ((key, pair) in LectionaryMap.psalmMap) {
            // pair.first to Sigla (np. "Ps 23, 1-4")
            // pair.second to Refren (tego nie pobieramy, bo już mamy w mapie)
            val sigla = pair.first
            downloadIfNeeded(sigla)
        }

        Log.d("PsalmScraper", "KONIEC: Wszystkie psalmy przetworzone!")
    }

    private suspend fun downloadIfNeeded(rawSigla: String) {
        // Czyszczenie wstępne: Czasami źródło dodaje "Ps: " przed inną księgą (np. "Ps: Is 12")
        // Usuwamy "Ps:" tylko jeśli jest na początku i ma dwukropek
        val sigla = rawSigla.replace(Regex("^Ps:\\s*"), "").trim()

        if (sigla.length < 3) return

        // Sprawdzamy czy mamy już treść tego psalmu w bazie
        val existingText = psalmDao.getPsalm(rawSigla) // Używamy oryginalnego klucza z mapy jako ID

        // Jeśli tekstu nie ma lub jest podejrzanie krótki -> POBIERAMY
        if (existingText.isNullOrEmpty() || existingText.length < 20) {

            val textFromWeb = downloadPsalmBySigla(sigla)

            if (textFromWeb.isNotEmpty() && textFromWeb.length > 10) {
                // Zapisujemy w bazie: Klucz = Sigla z mapy, Treść = Pobrany tekst
                psalmDao.insert(PsalmEntity(rawSigla, textFromWeb))
                Log.d("PsalmScraper", "Zapisano [$rawSigla]")
            } else {
                Log.e("PsalmScraper", "PUSTE/BŁĄD dla $rawSigla")
            }

            delay(500) // Opóźnienie
        }
    }

    private fun downloadPsalmBySigla(rawSigla: String): String {
        return try {
            // 1. Formatowanie URL
            var siglaClean = rawSigla
                .replace("–", "-")
                .replace("—", "-")
                .trim()

            // Usuwamy spacje
            siglaClean = siglaClean.replace(" ", "")

            // POPRAWKA 1: Obsługa ksiąg z cyfrą (np. 1Sm, 1Krn) oraz zwykłych (Ps, Iz)
            // Wstawiamy spację między nazwę księgi a rozdział (np. "1Sm2" -> "1Sm 2", "Ps23" -> "Ps 23")
            // Regex łapie: (opcjonalna cyfra + litery) i (cyfry)
            siglaClean = siglaClean.replace(Regex("^(\\d?[A-Za-zŁŚŻŹĆĘÓŃłśżźćęóń]+)(\\d+)"), "$1 $2")

            // POPRAWKA 2: USUNĄŁEM kod usuwający literki (a, b) przy wersetach.
            // wBiblii.pl radzi sobie z "2a", a usunięcie ich powodowało błędy duplikatów.

            val encodedSigla = URLEncoder.encode(siglaClean, "UTF-8")
            val url = "https://wbiblii.pl/szukaj/$encodedSigla"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .timeout(30000)
                .get()

            // --- 2. CZYSZCZENIE HTML (Bez zmian) ---
            doc.select(".audio-player, .btn, .navigation, .nav-links, .footer, #footer").remove()
            doc.select("a:contains(Odsłuchaj)").remove()
            doc.select("div:contains(Odsłuchaj)").remove()
            doc.select("div.titles").remove()
            doc.select("div.footnotes").remove()
            doc.select("sup").remove()
            doc.select("strong.cap").remove()

            // --- 3. POBIERANIE TEKSTU (Bez zmian) ---
            val versesContainer = doc.select("div.verses").first()

            var text = if (versesContainer != null) {
                versesContainer.text()
            } else {
                doc.select("p").filter {
                    !it.text().contains("Dodaj do ulubionych") && !it.text().contains("Odsłuchaj")
                }.joinToString(" ") { it.text() }
            }

            // --- 4. ODCINANIE OGONÓW (Bez zmian) ---
            val cutOffMarkers = listOf("Odsłuchaj", "Przypisy", "←", "→", "Szukając po słowach")
            for (marker in cutOffMarkers) {
                if (text.contains(marker)) {
                    val index = text.lastIndexOf(marker)
                    if (index > 0) { // Zabezpieczenie
                        text = text.substring(0, index).trim()
                    }
                }
            }

            // Ostateczne sprawdzenie czy nie pobraliśmy strony błędu
            if (text.contains("Twoje sigla są błędne") || text.contains("Nie znaleziono")) {
                return "" // Zwracamy puste, żeby nie zapisało śmieci do bazy
            }

            return text.replace("\\s+".toRegex(), " ").trim()

        } catch (e: Exception) {
            Log.e("PsalmScraper", "Błąd sieci dla $rawSigla: ${e.message}")
            ""
        }
    }
}