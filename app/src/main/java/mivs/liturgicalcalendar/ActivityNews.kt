package mivs.liturgicalcalendar

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.remote.RetrofitClient

class ActivityNews : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var adView: AdView? = null
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)

        changeNaviBarColor()
        setupEdgeToEdge()
        initUI()
        fetchNewsFromApi()

        analytics = Firebase.analytics
    }

    private fun changeNaviBarColor() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightNavigationBars = false
        controller.isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.BLACK
    }

    private fun setupEdgeToEdge() {
        val mainRoot = findViewById<View>(R.id.main)
        val topToolbar = findViewById<View>(R.id.topToolbar)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            topToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            adContainerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }
            insets
        }
    }

    private fun initUI() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView = findViewById(R.id.recyclerViewNews)
        progressBar = findViewById(R.id.progressBarNews)

        // Ukrywamy listę na starcie, aż dane będą gotowe
        recyclerView.visibility = View.GONE
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<FrameLayout>(R.id.adContainer).post { setupAds() }
    }

    private fun fetchNewsFromApi() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Dodajemy timeout 30 sekund (30 000 ms)
                val response = withTimeout(30000L) {
                    RetrofitClient.instance.getNewsFeed()
                }

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val newsList = response.body() ?: emptyList()

                    if (newsList.isEmpty()) {
                        Toast.makeText(this@ActivityNews, R.string.no_news, Toast.LENGTH_SHORT).show()
                    } else {
                        recyclerView.adapter = NewsAdapter(newsList)
                        // Pokazujemy zawartość dopiero gdy jest gotowa
                        recyclerView.visibility = View.VISIBLE
                    }
                } else {
                    val errorMsg = getString(R.string.error_server, response.code())
                    Toast.makeText(this@ActivityNews, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: TimeoutCancellationException) {
                // Obsługa błędu po 30 sekundach braku odpowiedzi
                progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityNews, getString(R.string.error_connection), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityNews, getString(R.string.error_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        val adLayout = findViewById<View>(R.id.adContainerLayout)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adLayout.visibility = View.GONE
                adView?.destroy()
                adView = null
            } else {
                adLayout.visibility = View.VISIBLE
                if (adView == null) {
                    val newAdView = AdView(this)
                    newAdView.adUnitId = BuildConfig.AD_BANNER_ID
                    newAdView.setAdSize(getAdSize(adContainer))
                    adView = newAdView
                    adContainer.addView(newAdView)
                    newAdView.loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    private fun getAdSize(container: FrameLayout): AdSize {
        val density = resources.displayMetrics.density
        var width = container.width.toFloat()
        if (width == 0f) width = resources.displayMetrics.widthPixels.toFloat()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (width / density).toInt())
    }

    override fun onResume() {
        super.onResume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "ActivityNews")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivityNews")
        }
    }
}