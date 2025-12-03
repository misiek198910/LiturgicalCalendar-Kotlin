package mivs.liturgicalcalendar.ui.calendar

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.ui.details.ReadingsBottomSheet // <--- IMPORT
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(repository = CalendarRepository(requireContext()))
    }

    private lateinit var calendarView: CalendarView
    private lateinit var gospelContainer: View

    // --- POLA KLASY (Widoczne w całej klasie) ---
    private lateinit var dateText: TextView
    private lateinit var feastText: TextView
    private lateinit var seasonText: TextView
    private lateinit var gospelSigla: TextView
    private lateinit var psalmText: TextView
    // -------------------------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        calendarView.setCalendarDayLayout(R.layout.calendar_day_cell)

        gospelContainer = view.findViewById(R.id.gospelContainer)

        // Inicjalizacja zmiennych
        dateText = view.findViewById(R.id.dateText)
        feastText = view.findViewById(R.id.feastText)
        seasonText = view.findViewById(R.id.seasonText)
        gospelSigla = view.findViewById(R.id.gospelSigla)
        psalmText = view.findViewById(R.id.psalmText)

        setupObservers()
        setupListeners()

        val currentPage = calendarView.currentPageDate
        viewModel.loadMonthData(currentPage)
        viewModel.onDaySelected(java.util.Calendar.getInstance())
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { events ->
                    calendarView.setEvents(events)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state?.let { uiState ->
                        val finalFeastName = uiState.readings.dbFeastName ?: uiState.day.feastName
                        val dayToDisplay = uiState.day.copy(feastName = finalFeastName)

                        updateDetails(dayToDisplay)

                        // Tu już nie ma błędów, bo używamy zmiennych klasy
                        gospelSigla.text = uiState.readings.gospelSigla
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
        calendarView.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() { viewModel.loadMonthData(calendarView.currentPageDate) }
        })
        calendarView.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() { viewModel.loadMonthData(calendarView.currentPageDate) }
        })
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: com.applandeo.materialcalendarview.EventDay) {
                viewModel.onDaySelected(eventDay.calendar)
            }
        })

        // Kliknięcie w Ewangelię
        gospelContainer.setOnClickListener {
            val currentState = viewModel.uiState.value

            if (currentState != null) {
                val sigla = currentState.readings.gospelSigla
                val content = currentState.readings.gospelFullText // Teraz Repository to zwraca!

                if (!content.isNullOrEmpty()) {
                    val bottomSheet = ReadingsBottomSheet.newInstance(
                        sigla = "Ewangelia: $sigla",
                        content = content
                    )
                    bottomSheet.show(parentFragmentManager, "ReadingsSheet")
                } else {
                    Toast.makeText(requireContext(), "Pobieranie tekstu...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}