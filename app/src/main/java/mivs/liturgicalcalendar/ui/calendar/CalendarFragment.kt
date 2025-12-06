package mivs.liturgicalcalendar.ui.calendar

import android.os.Bundle
import android.view.View
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

        // UWAGA: loadBannerAd() jest teraz wywoływane w observerze isPremium!
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

                        val hasGospel = !uiState.readings.gospelFullText.isNullOrEmpty()
                        val hasPsalm = !uiState.readings.psalmFullText.isNullOrEmpty()

                        gospelContainer.visibility = if (hasGospel) View.VISIBLE else View.GONE
                        psalmContainer.visibility = if (hasPsalm) View.VISIBLE else View.GONE
                    }
                }
            }
        }

    }

    private fun updateDetails(day: mivs.liturgicalcalendar.domain.model.LiturgicalDay) {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pl"))
        dateText.text = day.date.format(formatter)
        feastText.text = day.feastName ?: "Dzień powszedni"

        // 1. Ustawienie tekstu okresu
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
            if (currentState != null && !currentState.readings.gospelFullText.isNullOrEmpty()) {
                val bottomSheet = ReadingsBottomSheet.newInstance(
                    sigla = "Ewangelia: ${currentState.readings.gospelSigla}",
                    content = currentState.readings.gospelFullText!!
                )
                bottomSheet.show(parentFragmentManager, "ReadingsSheet")
            } else {
                Toast.makeText(requireContext(), "Brak treści Ewangelii.", Toast.LENGTH_SHORT).show()
            }
        }

        psalmContainer.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState != null && !currentState.readings.psalmFullText.isNullOrEmpty()) {
                val bottomSheet = ReadingsBottomSheet.newInstance(
                    sigla = "Psalm: ${currentState.readings.psalmSigla}",
                    content = currentState.readings.psalmFullText!!
                )
                bottomSheet.show(parentFragmentManager, "ReadingsSheet")
            } else {
                Toast.makeText(requireContext(), "Brak pełnej treści Psalmu w bazie.", Toast.LENGTH_SHORT).show()
            }
        }

    }
}