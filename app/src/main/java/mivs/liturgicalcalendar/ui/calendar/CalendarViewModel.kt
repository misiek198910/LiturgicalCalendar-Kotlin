package mivs.liturgicalcalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.applandeo.materialcalendarview.EventDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.billing.SubscriptionStatus
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.domain.model.LiturgicalDay
import mivs.liturgicalcalendar.ui.common.LiturgicalToEventMapper
import java.time.LocalDate
import java.util.Calendar

class CalendarViewModel(
    private val repository: CalendarRepository,
    private val subscriptionManager: SubscriptionManager) : ViewModel() {

    private var adShownOnExit = false
    var isInternalNavigation = false

    fun triggerExitAd(onShowAd: () -> Unit) {
        // Reklama tylko dla darmowych użytkowników
        if (_isPremium.value == false) {
            adShownOnExit = true
            onShowAd()
        }
    }

    fun triggerResumeAd(onShowAd: () -> Unit) {
        if (_isPremium.value == false && !adShownOnExit && !isInternalNavigation) {
            onShowAd()
        }
        // Zawsze resetujemy flagi
        adShownOnExit = false
        isInternalNavigation = false
    }
    private val _events = MutableStateFlow<List<EventDay>>(emptyList())
    val events: StateFlow<List<EventDay>> = _events
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    init {
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
            val dayInfo = repository.getDay(date)
            val readings = repository.getReadingsForDay(dayInfo)
            _uiState.emit(CalendarUiState(dayInfo, readings))
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