# Specyfikacja Projektowa: Widget Kalendarza Liturgicznego (Android)

## 1. Przegląd Projektu
Celem projektu jest stworzenie natywnej aplikacji na system Android z Kalendarzem Liturgicznym, której główną wartością użytkową są **widgety na ekran główny (Home Screen)** oraz **ekran blokady (Lock Screen)**.

---

## 2. Wytyczne Techniczne i Architektura

* **UI Widgetu:** Jetpack Glance (deklaratywne tworzenie widgetów w oparciu o Compose API).
* **Architektura Danych:** Offline-first. 
* **Aktualizacje w tle:** `WorkManager` oraz `GlanceAppWidgetManager` (odświeżanie automatyczne o północy oraz przy zmianie strefy czasowej/daty).
* **Monetyzacja:** Model **Freemium** (darmowa wersja podstawowa + jednorazowy zakup In-App lub subskrypcja Premium).
* **Reklamy:** Brak reklam na widgetach (zgodnie z polityką Google AdMob i ograniczeniami `RemoteViews`). Reklamy (AdMob Banner / Native) występują wyłącznie wewnątrz głównej aplikacji.

---

## 3. Podział Funkcjonalności (Freemium)

| Cecha / Funkcja | Wersja Darmowa (Free) | Wersja Płatna (Premium) |
| :--- | :--- | :--- |
| **Dostępne rozmiary** | Mały ($2 \times 2$), Średni ($4 \times 2$) | Wszystkie + Duży ($4 \times 4$) oraz Lock Screen |
| **Zakres informacji** | Data, nazwa dnia/święta, kolor liturgiczny, sigla czytań (np. *Mt 9, 9-13*) | Pełny zakres + informacje o poście/wstrzemięźliwości, patron dnia, liturgia godzin |
| **Treść na pulpicie** | Brak treści (tylko odnośniki/sigla) | **Cytat z Ewangelii lub Myśl Dnia** bezpośrednio na pulpicie |
| **Personalizacja** | Standardowy motyw Jasny / Ciemny | Dynamiczne tło zależne od koloru szat (zielony, fioletowy, biały, czerwony), regulacja przezroczystości |
| **Interaktywność** | Kliknięcie otwiera aplikację | Nawigacja na widgecie (podgląd jutra/wczoraj bez otwierania appki) |

---

## 4. Layouty Widgetów

### A. Mały Widget ($2 \times 2$)
* **Free:** Data, dzień tygodnia, skrócona nazwa dnia/święta, pasek koloru liturgicznego.
* **Premium:** Dodatkowo ikona/status posta oraz ranga dnia.

### B. Średni Widget ($4 \times 2$)
* **Free:** Kolor liturgiczny + data + wykaz czytań (sigla).
* **Premium:** Wykaz czytań + wyróżniony fragment Ewangelii dnia + status wstrzemięźliwości od potraw mięsnych.

### C. Duży Widget ($4 \times 4$) – *Premium Only*
* Pełny zestaw informacji liturgicznych, tekst Ewangelii, patron dnia, interaktywne strzałki przełączania dni.

---

## 5. Uwaga Techniczna: Integracja Glance z aplikacją opartą o XML/Views

Istniejąca aplikacja jest w całości zbudowana na klasycznym systemie widoków Androida (`Activity`/`Fragment` + layouty XML, `res/layout`), **bez żadnej zależności od Jetpack Compose**. Punkt 2 zakłada użycie **Jetpack Glance** do budowy widgetów, co mogłoby sugerować konieczność przepisania aplikacji na Compose — **to nieporozumienie, którego nie trzeba się obawiać**:

* Widgety na ekran główny zawsze renderują się przez mechanizm zbliżony do `RemoteViews`, niezależnie od tego, jakiego frameworka UI używa reszta aplikacji. Glance jest właśnie warstwą, która generuje ten `RemoteViews` z kodu deklaratywnego (`@Composable`).
* Glance korzysta jedynie z *Compose Runtime/Graphics/Unit* (fundamentów), a **nie** z Jetpack Compose UI (`androidx.compose.ui`, `androidx.compose.material3` do zwykłych ekranów). Dokumentacja Androida wprost zaznacza, że Glance nie jest interoperacyjny z resztą Compose UI i nie należy ich mieszać — co w naszym przypadku jest naturalnym stanem rzeczy, bo reszta appki i tak zostaje na XML.
* W praktyce integracja sprowadza się do:
  1. Dodania w `app/build.gradle.kts` pluginu kompilatora Compose (`org.jetbrains.kotlin.plugin.compose`, wspierany od Kotlina 2.0 — projekt już jest na Kotlinie 2.0.21) oraz `android.buildFeatures.compose = true`.
  2. Dodania zależności `androidx.glance:glance-appwidget` (+ opcjonalnie `glance-material3` do komponentów Material).
  3. Utworzenia osobnego pakietu `widget/` z klasami `GlanceAppWidget` i `GlanceAppWidgetReceiver` — całkowicie odizolowanego od istniejących `Activity`/`Fragment`.
  4. Rejestracji odbiornika widgetu w `AndroidManifest.xml` (analogicznie do zwykłego `AppWidgetProvider`) i pliku metadanych w `res/xml/`.
* Dane do widgetu (dzień liturgiczny, kolor, sigla czytań) pobieramy z istniejącego `CalendarRepository`/Room — ta sama baza danych i logika (`LiturgicalCalendarCalc`) co w głównej aplikacji, żadnego duplikowania.
* Koszt tego podejścia to wyłącznie nieco większy rozmiar APK (transitywna zależność od Compose runtime) — architektura reszty aplikacji (XML, Fragmenty, ViewModel) pozostaje bez zmian.

### Odświeżanie o północy oraz przy zmianie strefy/daty

* **Północ:** `WidgetRefreshWorker` (`CoroutineWorker`, ten sam wzorzec co istniejący `DailyFeastWorker`) zaplanowany jako periodic work co 24h z opóźnieniem startowym wyliczonym do najbliższej lokalnej północy. Wołany `CalendarWidget().updateAll(context)`. Planowany/anulowany w `CalendarWidgetReceiver.onEnabled/onDisabled` — działa tylko, gdy użytkownik faktycznie ma widget na pulpicie.
* **Zmiana strefy czasowej / ręczna zmiana zegara:** osobny `WidgetDateChangeReceiver` zarejestrowany w manifeście na `TIMEZONE_CHANGED` i `TIME_SET` (jedyne dwa z tej rodziny akcji zwolnione z ograniczeń niejawnych broadcastów na API 26+ — `DATE_CHANGED` nie jest zwolniony, więc zadeklarowanie go w manifeście byłoby martwym kodem na nowszych Androidach; zmianę daty o północy pokrywa już powyższy worker).

### Dwa osobne widgety w pickerze (Free / Premium)

Zamiast jednego widgetu, który sam sobie sprawdza uprawnienia, w pickerze na pulpicie są **dwie osobne pozycje**, każda to własny `GlanceAppWidget` + `GlanceAppWidgetReceiver` + plik metadanych w `res/xml/`:

* **`CalendarWidgetFree`** (2×2) — dokładnie to, co opisane w pkt 4A kolumna Free: data, dzień tygodnia, skrócona nazwa dnia/święta, pasek koloru liturgicznego.
* **`CalendarWidgetPremium`** (4×2) — data, pełna nazwa dnia/święta, wyróżniony cytat z Ewangelii (`CalendarRepository.getReadingsForDay`), status wstrzemięźliwości od pokarmów mięsnych (reguła: każdy piątek poza okresem wielkanocnym i uroczystościami — nie ma tego pola w bazie feastów, więc liczone z reguły kanonicznej, nie zmyślane). Widget jest widoczny w pickerze dla każdego, ale status subskrypcji (`UserStatusDao` w Room, ten sam cache co reszta appki) sprawdzany jest dopiero przy renderowaniu — bez aktywnej subskrypcji pokazuje się zachęta do wykupienia zamiast treści.
* Oba typy dzielą wspólne helpery (`WidgetCommon.kt`: kolor liturgiczny, sprawdzenie premium, reguła wstrzemięźliwości) oraz jeden `WidgetRefreshWorker`/`WidgetDateChangeReceiver` odświeżający obie instancje naraz — usunięcie jednego typu widgetu z pulpitu nie wyłącza odświeżania drugiego (sprawdzane przez `GlanceAppWidgetManager.getGlanceIds` dla obu typów przed anulowaniem workera).
* **Świadomie pominięte na stałe:** patron dnia i liturgia godzin (pkt 3) — decyzja produktowa, nie tymczasowy brak: nazwa dnia (np. "Św. Mateusza Apostoła i Ewangelisty") już pełni tę funkcję, a liturgii godzin nie ma w bazie i nie planujemy jej dodawać na razie.
* **Nawigacja strzałkami wczoraj/jutra** (pkt 3, wiersz "Interaktywność" → Premium) — zaimplementowana. Przeglądany dzień trzymany jest jako offset (-3..+3) od "dziś" w stanie widgetu (`PreferencesGlanceStateDefinition`, osobno dla każdej instancji). Strzałki (`ic_arrow_back`/`ic_arrow_forward`) wołają `ActionCallback` (`PreviousDayAction`/`NextDayAction`), które aktualizują offset i wymuszają odświeżenie tej jednej instancji. `WidgetRefreshWorker` resetuje offset do 0 przy każdym odświeżeniu o północy, żeby po zmianie dnia widget nie został "zamrożony" na nieaktualnym podglądzie. Dostępna tylko w wersji Premium.
* **Nie zaimplementowane jeszcze:** dynamiczne tło zależne od przezroczystości (pkt 3, "Personalizacja" → Premium) — samo dynamiczne tło zależne od koloru szat już działa (pasek/tło reaguje na `colorCode`), ale regulacja przezroczystości nie.

### Status na koniec sesji (do kontynuacji)

* **Nawigacja strzałkami wczoraj/jutra — wycofana.** Mechanizm technicznie działał (potwierdzone na realnym telefonie), ale opóźnienie restartu "sesji" Glance (kilka sekund między tapem a zmianą treści) sprawiało wrażenie zepsutej funkcji. Na życzenie usunięte — `CalendarWidgetPremium` pokazuje teraz na stałe dzisiejszy dzień: datę, święto, **Psalm** (sigla+refren) i **cytat Ewangelii** pod spodem, bez elementów interaktywnych poza kliknięciem całego widgetu (otwiera aplikację).
* **Bug: widget Premium "gubił" status mimo aktywnej subskrypcji — przyczyna znaleziona i naprawiona w `BillingManager.kt`.** `queryPurchasesAsync()` bezwarunkowo nadpisywał Room (`isPremium=false`), gdy pojedyncze zapytanie do Play Billing nie znalazło aktywnego zakupu — a lokalny cache Play Billing bywa chwilowo pusty tuż po starcie usługi. Naprawa: przed zdemotowaniem sprawdzamy trwały stan w Room (`dao.getStatus()`, nie `_isPremium.value` z LiveData — to była pierwsza, wadliwa wersja poprawki, bo LiveData startuje zawsze od `false` i race'owała) i jeśli lokalnie był premium, czekamy 2s i próbujemy jeszcze raz, zanim uznamy subskrypcję za wygasłą. **Wymaga potwierdzenia w realnym użyciu przez kilka dni** — trudno to wiarygodnie odtworzyć na żądanie, więc trzeba obserwować, czy problem faktycznie już nie wraca.
* **Przyciski kup subskrypcję — ukrywane (nie tylko wyszarzane) przy aktywnej subskrypcji**, zaimplementowane w `SubscriptionActivity.kt` i zweryfikowane na telefonie.
* **W trakcie:** powiększenie wykorzystania powierzchni widgetu Premium — treść wyśrodkowana w pionie (`Alignment.CenterVertically`), obcinanie Psalmu/Ewangelii znacznie poluzowane (cap tylko jako zabezpieczenie, realne ograniczenie ma teraz dawać naturalny reflow RemoteViews przy zmianie rozmiaru widgetu przez użytkownika — `resizeMode="horizontal|vertical"` już ustawiony w metadanych). **Zbudowane i wgrane na telefon, ale nie zdążyliśmy dokończyć wizualnej weryfikacji** (m.in. czy dłuższy tekst faktycznie się pokazuje po ręcznym powiększeniu widgetu na pulpicie) — do zrobienia na początku następnej sesji.
* Ostatni stan bazy na telefonie testowym: `isPremium` ręcznie przywrócone na `true` (prawdziwa aktywna subskrypcja), ale widget Premium usunięty z pulpitu tuż przed przerwaniem sesji (w trakcie testu "usuń i dodaj ponownie, żeby wymusić świeży odczyt") — **trzeba go z powrotem dodać na pulpit na początku kolejnej sesji**, żeby kontynuować weryfikację.

---