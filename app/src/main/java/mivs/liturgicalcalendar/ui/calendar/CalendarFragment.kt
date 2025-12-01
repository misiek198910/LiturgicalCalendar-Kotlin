package mivs.liturgicalcalendar.ui.calendar

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    // Tworzymy ViewModel przy użyciu naszej Fabryki
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(repository = CalendarRepository(requireContext()))
    }

    private lateinit var calendarView: CalendarView

    // Pola tekstowe
    private lateinit var dateText: TextView
    private lateinit var feastText: TextView
    private lateinit var seasonText: TextView
    // NOWE POLA:
    private lateinit var gospelText: TextView
    private lateinit var psalmText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Znajdujemy widoki
        calendarView = view.findViewById(R.id.calendarView)
        calendarView.setCalendarDayLayout(R.layout.calendar_day_cell)

        dateText = view.findViewById(R.id.dateText)
        feastText = view.findViewById(R.id.feastText)
        seasonText = view.findViewById(R.id.seasonText)
        // Znajdujemy nowe pola do czytań
        gospelText = view.findViewById(R.id.gospelText)
        psalmText = view.findViewById(R.id.psalmText)

        setupObservers()
        setupListeners()

        // Załaduj dane na start (ikony w kalendarzu)
        val currentPage = calendarView.currentPageDate
        viewModel.loadMonthData(currentPage)

        // Zaznacz "dzisiaj" w informacjach na dole
        viewModel.onDaySelected(java.util.Calendar.getInstance())
    }

    private fun setupObservers() {
        // 1. Obserwujemy strumień ikon (kropki/obrazki w kalendarzu)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { events ->
                    calendarView.setEvents(events)
                }
            }
        }

        // 2. Obserwujemy SZCZEGÓŁY DNIA (Dzień + Czytania)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // state to obiekt CalendarUiState (lub null)
                    state?.let { uiState ->

                        // --- POPRAWKA LOGIKI NAZW ---
                        // Sprawdzamy: czy w bazie (readings) jest nazwa święta?
                        // Jeśli tak -> użyj jej. Jeśli nie -> użyj tej z algorytmu.
                        val finalFeastName = uiState.readings.dbFeastName ?: uiState.day.feastName

                        // Tworzymy kopię dnia z poprawną nazwą, żeby przekazać do metody wyświetlającej
                        val dayToDisplay = uiState.day.copy(feastName = finalFeastName)

                        // Aktualizujemy nagłówki (Data, Święto, Sezon)
                        updateDetails(dayToDisplay)

                        // --- POPRAWKA TEKSTU ---
                        // Używamy zmiennych lokalnych (gospelText, psalmText), a nie 'binding'
                        gospelText.text = "Ewangelia: ${uiState.readings.gospelSigla}"
                        psalmText.text = "Psalm: ${uiState.readings.psalmResponse}"
                    }
                }
            }
        }
    }

    private fun updateDetails(day: mivs.liturgicalcalendar.domain.model.LiturgicalDay) {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pl"))
        dateText.text = day.date.format(formatter)

        feastText.text = day.feastName ?: "Dzień powszedni"

        seasonText.text = when(day.season) {
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.ADVENT -> "Adwent"
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.CHRISTMAS -> "Boże Narodzenie"
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.LENT -> "Wielki Post"
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.EASTER -> "Okres Wielkanocny"
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.ORDINARY_TIME -> "Okres Zwykły"
            else -> ""
        }
    }

    private fun setupListeners() {
        // Obsługa przesuwania miesięcy
        calendarView.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                viewModel.loadMonthData(calendarView.currentPageDate)
            }
        })

        calendarView.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                viewModel.loadMonthData(calendarView.currentPageDate)
            }
        })

        // Obsługa kliknięcia w dzień
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: com.applandeo.materialcalendarview.EventDay) {
                viewModel.onDaySelected(eventDay.calendar)
            }
        })
    }
}