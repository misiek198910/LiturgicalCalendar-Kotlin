package mivs.liturgicalcalendar

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.switchmaterial.SwitchMaterial
import mivs.liturgicalcalendar.billing.BillingManager
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.data.preferences.PreferencesManager
import mivs.liturgicalcalendar.worker.DailyFeastWorker
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity(), BillingManager.BillingManagerListener {

    private lateinit var billingManager: BillingManager
    private lateinit var preferencesManager: PreferencesManager

    // Widoki
    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout

    // Karta Premium
    private lateinit var cardPremium: CardView
    private lateinit var imgPremiumIcon: ImageView
    private lateinit var txtPremiumTitle: TextView
    private lateinit var txtPremiumSubtitle: TextView

    // Powiadomienia i Picker
    private lateinit var switchNotification: SwitchMaterial
    private lateinit var iconLock: ImageView
    private lateinit var layoutNotificationOption: RelativeLayout
    private lateinit var layoutTimePicker: LinearLayout
    private lateinit var txtTimeDisplay: TextView

    private lateinit var btnPrivacyPolicy: LinearLayout

    private var adView: AdView? = null
    private var isPremiumUser = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableNotifications()
        } else {
            switchNotification.isChecked = false
            Toast.makeText(this, "Brak zgody na powiadomienia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        MobileAds.initialize(this) {}
        billingManager = SubscriptionManager.getInstance(applicationContext).billingManager
        billingManager.setListener(this)
        preferencesManager = PreferencesManager(this)

        initViews()
        setupListeners()
        updateTimeDisplay() // Wyświetl zapisaną godzinę

        // OBSERWACJA PREMIUM
        billingManager.isPremium.observe(this) { isPremium ->
            isPremiumUser = isPremium
            updatePremiumUI(isPremium)
            handleAds(isPremium)
        }
    }

    private fun initViews() {
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        cardPremium = findViewById(R.id.cardPremium)
        imgPremiumIcon = findViewById(R.id.imgPremiumIcon)
        txtPremiumTitle = findViewById(R.id.txtPremiumTitle)
        txtPremiumSubtitle = findViewById(R.id.txtPremiumSubtitle)

        switchNotification = findViewById(R.id.switchNotification)
        iconLock = findViewById(R.id.iconLock)
        layoutNotificationOption = findViewById(R.id.layoutNotificationOption)

        layoutTimePicker = findViewById(R.id.layoutTimePicker)
        txtTimeDisplay = findViewById(R.id.txtTimeDisplay)

        btnPrivacyPolicy = findViewById(R.id.btnPrivacyPolicy)

        switchNotification.isChecked = preferencesManager.areNotificationsEnabled

        // Pokaż/Ukryj picker w zależności od stanu włącznika
        layoutTimePicker.visibility = if (preferencesManager.areNotificationsEnabled) View.VISIBLE else View.GONE

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupListeners() {
        // Kliknięcie w kartę Premium -> Otwiera Subskrypcje (TO ZOSTAWIAMY)
        cardPremium.setOnClickListener { openSubscriptionScreen() }

        // Kliknięcie w rząd powiadomień (Kłódka)
        layoutNotificationOption.setOnClickListener {
            if (!isPremiumUser) {
                // ZMIANA 1: Tylko Toast, brak przejścia do innej aktywności
                Toast.makeText(this, "Funkcja dostępna w wersji Premium", Toast.LENGTH_SHORT).show()
            } else {
                switchNotification.toggle()
                handleNotificationToggle(switchNotification.isChecked)
            }
        }

        // Kliknięcie bezpośrednio w suwak
        switchNotification.setOnClickListener {
            if (!isPremiumUser) {
                switchNotification.isChecked = false
                // ZMIANA 1: Tylko Toast, brak przejścia do innej aktywności
                Toast.makeText(this, "Funkcja dostępna w wersji Premium", Toast.LENGTH_SHORT).show()
            } else {
                handleNotificationToggle(switchNotification.isChecked)
            }
        }

        // Listener Pickera Godziny
        layoutTimePicker.setOnClickListener {
            showTimePickerDialog()
        }

        btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }
    }

    // --- ZARZĄDZANIE WYGLĄDEM PREMIUM ---
    private fun updatePremiumUI(isPremium: Boolean) {
        if (isPremium) {
            // ZMIANA TREŚCI KARTY
            cardPremium.visibility = View.VISIBLE
            imgPremiumIcon.setImageResource(R.drawable.ic_lock_open) // Kłódka otwarta
            txtPremiumSubtitle.text = "Zarządzaj subskrypcją"

            // ODBLOKOWANIE UI
            iconLock.visibility = View.GONE
            switchNotification.visibility = View.VISIBLE

            // ZMIANA 2: Odśwież widoczność pickera godziny
            // Jeśli użytkownik ma włączone powiadomienia, pokaż picker od razu po wykryciu Premium
            layoutTimePicker.visibility = if (switchNotification.isChecked) View.VISIBLE else View.GONE

        } else {
            // TREŚĆ DLA FREE
            cardPremium.visibility = View.VISIBLE
            imgPremiumIcon.setImageResource(R.drawable.ic_lock) // Kłódka zamknięta
            txtPremiumSubtitle.text = "Kliknij, aby usunąć reklamy i włączyć Słowo"

            // ZABLOKOWANIE UI
            iconLock.visibility = View.VISIBLE
            switchNotification.visibility = View.GONE
            layoutTimePicker.visibility = View.GONE
        }
    }

    // --- LOGIKA CZASU (PICKER) ---
    private fun showTimePickerDialog() {
        val currentHour = preferencesManager.notificationHour
        val currentMinute = preferencesManager.notificationMinute

        val timePicker = TimePickerDialog(this,
            { _, hourOfDay, minute ->
                preferencesManager.notificationHour = hourOfDay
                preferencesManager.notificationMinute = minute

                updateTimeDisplay()
                scheduleWorker()
                Toast.makeText(this, "Zmieniono godzinę powiadomienia", Toast.LENGTH_SHORT).show()
            },
            currentHour, currentMinute, true
        )
        timePicker.show()
    }

    private fun updateTimeDisplay() {
        val hour = preferencesManager.notificationHour
        val minute = preferencesManager.notificationMinute
        txtTimeDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    // --- LOGIKA POWIADOMIEŃ ---

    private fun handleNotificationToggle(isChecked: Boolean) {
        if (isChecked) {
            checkAndEnableNotifications()
        } else {
            disableNotifications()
        }
    }

    private fun checkAndEnableNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                enableNotifications()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            enableNotifications()
        }
    }

    private fun enableNotifications() {
        preferencesManager.areNotificationsEnabled = true
        layoutTimePicker.visibility = View.VISIBLE // Pokaż picker
        scheduleWorker()
        Toast.makeText(this, "Powiadomienia włączone", Toast.LENGTH_SHORT).show()
    }

    private fun disableNotifications() {
        preferencesManager.areNotificationsEnabled = false
        layoutTimePicker.visibility = View.GONE // Ukryj picker
        WorkManager.getInstance(this).cancelUniqueWork("DailyFeastWork")
    }

    private fun scheduleWorker() {
        val initialDelay = calculateInitialDelay()

        val workRequest = PeriodicWorkRequestBuilder<DailyFeastWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyFeastWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = System.currentTimeMillis()

        val targetHour = preferencesManager.notificationHour
        val targetMinute = preferencesManager.notificationMinute

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= currentTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis - currentTime
    }

    private fun openSubscriptionScreen() {
        startActivity(Intent(this, SubscriptionActivity::class.java))
    }

    // --- REKLAMY ---
    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            if (adView != null) {
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            }
            adContainerLayout.visibility = View.GONE
        } else {
            if (adView == null) {
                adContainerLayout.visibility = View.VISIBLE
                loadBannerAd()
            }
        }
    }

    private fun loadBannerAd() {
        val adBannerId: String = BuildConfig.AD_BANNER_ID
        if (adBannerId.contains("BRAK_ID") || adBannerId.isEmpty()) {
            adContainerLayout.visibility = View.GONE
            return
        }
        val adView = AdView(this)
        adView.adUnitId = adBannerId
        adView.setAdSize(adSize)
        this.adView = adView
        adContainer.removeAllViews()
        adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainer.width.toFloat()
            if (adWidthPixels == 0f) adWidthPixels = outMetrics.widthPixels.toFloat()
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onPurchaseAcknowledged() {}
    override fun onPurchaseError(error: String?) {}
    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}