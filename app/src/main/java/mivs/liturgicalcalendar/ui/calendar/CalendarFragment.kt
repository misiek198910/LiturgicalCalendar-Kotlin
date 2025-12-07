package mivs.liturgicalcalendar.ui.calendar

import android.os.Bundle
import android.view.View
import android.widget.TextView
// Toast usunięty z importów, bo nie jest już potrzebny
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
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.ui.details.ReadingsBottomSheet
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(
            repository = CalendarRepository(requireContext()),
            subscriptionManager = SubscriptionManager.getInstance(requireContext())
        )
    }

    private lateinit var calendarView: CalendarView
    private lateinit var gospelContainer: View
    private lateinit var psalmContainer: View

    private lateinit var dateText: TextView
    private lateinit var feastText: TextView
    private lateinit var seasonText: TextView
    private lateinit var gospelSigla: TextView
    private lateinit var psalmSiglaDisplay: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        calendarView.setCalendarDayLayout(R.layout.calendar_day_cell)
        gospelContainer = view.findViewById(R.id.gospelContainer)
        psalmContainer = view.findViewById(R.id.psalmContainer)

        dateText = view.findViewById(R.id.dateText)
        feastText = view.findViewById(R.id.feastText)
        seasonText = view.findViewById(R.id.seasonText)
        gospelSigla = view.findViewById(R.id.gospelSigla)
        psalmSiglaDisplay = view.findViewById(R.id.psalmSiglaDisplay)

        setupObservers()
        setupListeners()
        val currentPage = calendarView.currentPageDate
        viewModel.loadMonthData(currentPage)
        viewModel.onDaySelected(Calendar.getInstance())
    }

    private fun setupObservers() {
        // Obserwacja zdarzeń kalendarza
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { events ->
                    calendarView.setEvents(events)
                }
            }
        }

        // Obserwacja szczegółów dnia
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state?.let { uiState ->
                        val finalFeastName = uiState.readings.dbFeastName ?: uiState.day.feastName
                        val dayToDisplay = uiState.day.copy(feastName = finalFeastName)

                        updateDetails(dayToDisplay)

                        gospelSigla.text = uiState.readings.gospelSigla
                        psalmSiglaDisplay.text = uiState.readings.psalmResponse

                        val gospelSiglaText = uiState.readings.gospelSigla
                        val psalmSiglaText = uiState.readings.psalmSigla

                        // Logika ukrywania: Pokaż, jeśli jest pełny tekst LUB jeśli jest znana sigla (nawet bez tekstu)
                        val showGospel = !uiState.readings.gospelFullText.isNullOrEmpty() ||
                                (gospelSiglaText.length > 3 && gospelSiglaText != "Patrz lekcjonarz" && gospelSiglaText != "Z dnia")

                        val showPsalm = !uiState.readings.psalmFullText.isNullOrEmpty() ||
                                (psalmSiglaText != null && psalmSiglaText.length > 2)

                        gospelContainer.visibility = if (showGospel) View.VISIBLE else View.GONE
                        psalmContainer.visibility = if (showPsalm) View.VISIBLE else View.GONE
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
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.TRIDUUM -> "Triduum Paschalne"
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.PENTECOST -> "Zesłanie Ducha Świętego"
        }

        val colorResId = when(day.season) {
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.ADVENT,
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.LENT -> R.color.liturgical_purple
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.CHRISTMAS,
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.EASTER -> R.color.liturgical_gold
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.TRIDUUM,
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.PENTECOST -> R.color.liturgical_red
            mivs.liturgicalcalendar.domain.model.LiturgicalSeason.ORDINARY_TIME -> R.color.liturgical_green
        }

        seasonText.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), colorResId))
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

        gospelContainer.setOnClickListener {
            val currentState = viewModel.uiState.value
            // Otwieramy tylko jeśli mamy pełny tekst. Jeśli nie ma, nic się nie dzieje (brak Toasta).
            if (currentState != null && !currentState.readings.gospelFullText.isNullOrEmpty()) {
                val bottomSheet = ReadingsBottomSheet.newInstance(
                    sigla = "Ewangelia: ${currentState.readings.gospelSigla}",
                    content = currentState.readings.gospelFullText!!
                )
                bottomSheet.show(parentFragmentManager, "ReadingsSheet")
            }
        }

        psalmContainer.setOnClickListener {
            val currentState = viewModel.uiState.value
            // Otwieramy tylko jeśli mamy pełny tekst. Jeśli nie ma, nic się nie dzieje (brak Toasta).
            if (currentState != null && !currentState.readings.psalmFullText.isNullOrEmpty()) {
                val bottomSheet = ReadingsBottomSheet.newInstance(
                    sigla = "Psalm: ${currentState.readings.psalmSigla}",
                    content = currentState.readings.psalmFullText!!
                )
                bottomSheet.show(parentFragmentManager, "ReadingsSheet")
            }
        }
    }
}