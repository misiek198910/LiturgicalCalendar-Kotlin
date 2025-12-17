package mivs.liturgicalcalendar

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.data.repository.CalendarRepository
import mivs.liturgicalcalendar.ui.calendar.CalendarViewModel
import mivs.liturgicalcalendar.ui.calendar.CalendarViewModelFactory
import mivs.liturgicalcalendar.ui.news.NewsActivity

class MainActivity : AppCompatActivity() {
    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(
            repository = CalendarRepository(this),
            subscriptionManager = SubscriptionManager.getInstance(applicationContext)
        )
    }

    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWindowInsets()

        MobileAds.initialize(this) {}

        val btnSettings = findViewById<View>(R.id.btnSettings)
        val btnAdsOf = findViewById<View>(R.id.btnRemoveAds)
        val btnNews = findViewById<View>(R.id.btnNews)


        btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnAdsOf?.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        btnNews?.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java))
        }

        val billingManager = SubscriptionManager.getInstance(applicationContext).billingManager
        billingManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                btnAdsOf.visibility = View.GONE // Ukryj, jeśli kupione
            } else {
                btnAdsOf.visibility = View.VISIBLE // Pokaż, jeśli darmowe
            }
        }

        setupAdsLogic()
    }


    private fun setupAdsLogic() {
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPremium.collect { isPremium ->
                    if (isPremium) {
                        // PREMIUM: Ukryj reklamy
                        hideBannerAd()
                    } else {
                        // FREE: Pokaż reklamy
                        showBannerAd()
                    }
                }
            }
        }
    }

    private fun showBannerAd() {
        if (adView != null) {
            adContainerLayout.visibility = View.VISIBLE
            return
        }

        adContainerLayout.visibility = View.VISIBLE

        adView = AdView(this)
        val adBannerId: String = BuildConfig.AD_BANNER_ID

        if (adBannerId.contains("BRAK_ID") || adBannerId.isEmpty()) {
            Log.e("AD_MOB", "BŁĄD: AD_BANNER_ID jest puste lub nieprawidłowe.")
            adContainerLayout.visibility = View.GONE
            return
        }
        adView?.adUnitId = adBannerId
        adView?.setAdSize(adSize)

        adContainer.removeAllViews()
        adContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)
    }

    private fun hideBannerAd() {
        adContainerLayout.visibility = View.GONE
        adContainer.removeAllViews()
        adView?.destroy()
        adView = null
    }

    private val adSize: AdSize
        get() {
            var adWidthPixels = adContainer.width.toFloat()

            // Jeśli kontener nie ma jeszcze wymiarów (np. przy starcie aplikacji)
            if (adWidthPixels == 0f) {
                // Sprawdzamy wersję Androida
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    // NOWY SPOSÓB (Android 11+)
                    val windowMetrics = windowManager.currentWindowMetrics
                    val bounds = windowMetrics.bounds
                    adWidthPixels = bounds.width().toFloat()
                } else {
                    // STARY SPOSÓB (Dla starszych Androidów)
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
            val adWidth = (adWidthPixels / density).toInt()

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
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
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}