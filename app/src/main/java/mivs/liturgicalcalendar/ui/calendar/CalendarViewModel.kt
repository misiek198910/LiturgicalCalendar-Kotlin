package mivs.liturgicalcalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.applandeo.materialcalendarview.EventDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager // Importuj SubscriptionManager
import mivs.liturgicalcalendar.billing.SubscriptionStatus
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.logic.LiturgicalCalendarCalc
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.ui.common.LiturgicalToEventMapper
import java.time.LocalDate
import java.util.Calendar

class CalendarViewModel(
    private val repository: CalendarRepository,
    private val subscriptionManager: SubscriptionManager // Dodajemy manager subskrypcji
) : ViewModel() {

    private val _events = MutableStateFlow<List<EventDay>>(emptyList())
    val events: StateFlow<List<EventDay>> = _events

    // Stan subskrypcji (obserwowany przez Fragment)
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    init {
        // Obserwujemy LiveData z SubscriptionManager i konwertujemy na StateFlow
        subscriptionManager.subscriptionStatus.observeForever { status ->
            _isPremium.value = (status == SubscriptionStatus.PREMIUM)
        }
    }

    fun loadMonthData(calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        viewModelScope.launch {
            val days = repository.getDaysForMonth(year, month)
            val mappedEvents = days.map { LiturgicalToEventMapper.map(it) }
            _events.emit(mappedEvents)
        }
    }

    // Szczegóły dnia
    data class CalendarUiState(
        val day: LiturgicalDay,
        val readings: CalendarRepository.DayReadings
    )

    private val _uiState = MutableStateFlow<CalendarUiState?>(null)
    val uiState: StateFlow<CalendarUiState?> = _uiState

    fun onDaySelected(calendar: Calendar) {
        val date = LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        viewModelScope.launch {
            val dayInfo = LiturgicalCalendarCalc.generateDay(date)
            val readings = repository.getReadingsForDay(dayInfo)
            _uiState.emit(CalendarUiState(dayInfo, readings))
        }
    }

    // Metoda do wywołania zakupu
    fun buyPremium(activity: android.app.Activity) {
        subscriptionManager.productDetails.value?.let { details ->
            subscriptionManager.billingManager.launchPurchaseFlow(activity, details)
        }
    }
}

class CalendarViewModelFactory(
    private val repository: CalendarRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, subscriptionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}