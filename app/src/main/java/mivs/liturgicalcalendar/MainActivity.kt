package mivs.liturgicalcalendar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.ui.calendar.CalendarViewModel
import mivs.liturgicalcalendar.ui.calendar.CalendarViewModelFactory

class MainActivity : AppCompatActivity() {
    private val calendarViewModel: CalendarViewModel by lazy {

        val repo = mivs.liturgicalcalendar.data.repository.CalendarRepository(applicationContext)
        val subManager = SubscriptionManager.getInstance(this)
        
        androidx.lifecycle.ViewModelProvider(
            this,
            CalendarViewModelFactory(repo, subManager)
        )[CalendarViewModel::class.java]
    }

    private var mInterstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    private lateinit var redDot: View
    private var adContainerLayout: FrameLayout? = null
    private var adContainer: FrameLayout? = null
    private var adView: AdView? = null
    private var latestNewsTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        changeNaviBarColor()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWindowInsets()

        MobileAds.initialize(this) {}

        val btnSettings = findViewById<View>(R.id.btnSettings)
        val btnAdsOf = findViewById<View>(R.id.btnRemoveAds)
        val btnNewsContainer = findViewById<View>(R.id.btnNewsContainer)
        redDot = findViewById(R.id.viewRedDot)


        btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            calendarViewModel.isInternalNavigation = true
        }

        btnAdsOf?.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
            calendarViewModel.isInternalNavigation = true
        }

        btnNewsContainer.setOnClickListener {
            redDot.visibility = View.GONE

            val prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE)

            val timeToSave =
                if (latestNewsTimestamp > 0) latestNewsTimestamp else System.currentTimeMillis()

            prefs.edit { putLong("last_checked_timestamp", timeToSave) }

            startActivity(Intent(this, ActivityNews::class.java))
            calendarViewModel.isInternalNavigation = true
        }

        val billingManager = SubscriptionManager.getInstance(applicationContext).billingManager
        billingManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                btnAdsOf.visibility = View.GONE
            } else {
                btnAdsOf.visibility = View.VISIBLE
            }
        }

        setupAdsLogic()

        loadInterstitialAd()

        // 2. Obsługa przycisku BACK (zamiast starego onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Sprawdzamy czy użytkownik NIE jest premium
                if (mInterstitialAd != null && calendarViewModel.isPremium.value == false) {
                    calendarViewModel.triggerExitAd {
                        mInterstitialAd?.show(this@MainActivity)
                        finish()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun changeNaviBarColor() {

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // 1. Wyłączamy jasny pasek nawigacji i włączamy ciemny
        controller.isAppearanceLightNavigationBars = false
        controller.isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.BLACK
    }

    private fun setupAdsLogic() {
        // Sprawdzamy, czy kontenery istnieją (w landscape mogą mieć visibility gone lub być usunięte)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        val subManager = SubscriptionManager.getInstance(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                subManager.isPremium.asFlow().collect { isPremium ->
                    val isPortrait = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

                    if (isPremium || !isPortrait) {
                        // Jeśli premium LUB tryb landscape -> ukrywamy reklamy
                        Log.d("ADS_LOGIC", "Ukrywam reklamy (Premium: $isPremium, Portrait: $isPortrait)")
                        hideBannerAd()
                    } else {
                        // Tylko jeśli użytkownik Free I tryb Portrait
                        Log.d("ADS_LOGIC", "Wyświetlam reklamy (Użytkownik Free, Portrait)")
                        showBannerAd()
                    }
                }
            }
        }
    }

    private fun showBannerAd() {
        // Jeśli kontenerów nie ma w XML (landscape), wychodzimy
        val layout = adContainerLayout ?: return
        val container = adContainer ?: return

        if (adView != null) {
            layout.visibility = View.VISIBLE
            return
        }

        layout.visibility = View.VISIBLE
        adView = AdView(this)

        val adBannerId: String = BuildConfig.AD_BANNER_ID
        if (adBannerId.contains("BRAK_ID") || adBannerId.isEmpty()) {
            layout.visibility = View.GONE
            return
        }

        adView?.let {
            it.adUnitId = adBannerId
            it.setAdSize(adSize)
            container.removeAllViews()
            container.addView(it)
            it.loadAd(AdRequest.Builder().build())
        }
    }

    private fun hideBannerAd() {
        adContainerLayout?.visibility = View.GONE
        adContainer?.removeAllViews()
        adView?.destroy()
        adView = null
    }

    private val adSize: AdSize get() {
        // Używamy bezpiecznego wywołania i elvis operatora ?: 0f na wypadek braku kontenera
        var adWidthPixels = adContainer?.width?.toFloat() ?: 0f

        // Jeśli kontener nie ma jeszcze szerokości (0f) lub w ogóle nie istnieje (null -> 0f)
        if (adWidthPixels == 0f) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                adWidthPixels = bounds.width().toFloat()
            } else {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                @Suppress("DEPRECATION")
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getMetrics(outMetrics)
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
        }

        val density = resources.displayMetrics.density
        // Dodajemy zabezpieczenie, żeby adWidth nie było zerem lub wartością ujemną
        val adWidth = if (adWidthPixels > 0) (adWidthPixels / density).toInt() else 320

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        val adId = "ca-app-pub-8612826840770530/3213905982"

        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, adId, adRequest,
            object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    mInterstitialAd = interstitialAd

                    mInterstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                            loadInterstitialAd()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
        checkNewsViaApi()
        calendarViewModel.triggerResumeAd {
            mInterstitialAd?.show(this)
        }
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    private fun checkNewsViaApi() {
        lifecycleScope.launch {
            try {
                val response = mivs.liturgicalcalendar.remote.RetrofitClient.instance.getNewsFeed()
                if (response.isSuccessful) {
                    val newsList = response.body() ?: emptyList()

                    val latestNews = newsList.firstOrNull { it.is_visible == true }

                    if (latestNews?.publish_date != null) {

                        val sdf = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        )
                        val date = sdf.parse(latestNews.publish_date)
                        val serverDateMillis = date?.time ?: 0L

                        latestNewsTimestamp = serverDateMillis

                        val prefs = getSharedPreferences("news_prefs", Context.MODE_PRIVATE)
                        val lastCheckedMillis = prefs.getLong("last_checked_timestamp", 0)

                        if (serverDateMillis > lastCheckedMillis) {
                            redDot.visibility = View.VISIBLE
                        } else {
                            redDot.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_CHECK", "Błąd parsowania daty lub połączenia", e)
            }
        }
    }
}
