package mivs.liturgicalcalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.applandeo.materialcalendarview.EventDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.ui.common.LiturgicalToEventMapper
import java.time.LocalDate
import java.util.Calendar

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    // --- DODAJEMY BLOK INIT TUTAJ ---
    init {
        viewModelScope.launch {
            // 1. Najpierw upewniamy się, że struktura bazy (dni, święta stałe/ruchome) istnieje
            repository.initializeData()

            // 2. Następnie uruchamiamy scrapera, żeby pobrał brakujące teksty Ewangelii (dla Cykli A, B, C)
            //repository.runScraper()


            //4. pobiera teksty psalmów
            //repository.runPsalmScraper()
        }
    }
    // -------------------------------

    // --- CZĘŚĆ 1: Kalendarz (Ikony na kratkach) ---
    private val _events = MutableStateFlow<List<EventDay>>(emptyList())
    val events: StateFlow<List<EventDay>> = _events

    fun loadMonthData(calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        viewModelScope.launch {
            // Pobieramy dni w tle i mapujemy na ikony
            val days = repository.getDaysForMonth(year, month)
            val mappedEvents = days.map { LiturgicalToEventMapper.map(it) }
            _events.emit(mappedEvents)
        }
    }

    // --- CZĘŚĆ 2: Szczegóły dnia (Panel na dole) ---

    // Jedna klasa stanu trzymająca komplet informacji
    data class CalendarUiState(
        val day: LiturgicalDay,
        val readings: CalendarRepository.DayReadings
    )

    // Strumień stanu, który obserwuje Fragment
    private val _uiState = MutableStateFlow<CalendarUiState?>(null)
    val uiState: StateFlow<CalendarUiState?> = _uiState

    fun onDaySelected(calendar: Calendar) {
        val date = LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        viewModelScope.launch {
            // 1. Obliczamy dane liturgiczne (np. "1. Niedziela Adwentu")
            val dayInfo = LiturgicalCalendarCalc.generateDay(date)

            // 2. Pobieramy czytania (z bazy lub internetu)
            val readings = repository.getReadingsForDay(dayInfo)

            // 3. Wysyłamy paczkę do widoku
            _uiState.emit(CalendarUiState(dayInfo, readings))
        }
    }
}

// Fabryka (bez zmian)
class CalendarViewModelFactory(private val repository: CalendarRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}