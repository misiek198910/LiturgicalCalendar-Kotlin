package mivs.liturgicalcalendar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        
        private const val AD_UNIT_ID = "ca-app-pub-8612826840770530/9888347667"
        private const val AD_TIMEOUT_MS = 8000L 
    }

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        changeNaviBarColor();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val subManager = SubscriptionManager.getInstance(applicationContext)


        subManager.billingManager.queryPurchasesAsync()

        lifecycleScope.launch {

            if (subManager.isPremiumValue) {
                Log.d(TAG, "Użytkownik Premium - pomijam RODO i reklamy")
                navigateToMainApp()
            } else {
                Log.d(TAG, "Użytkownik Free - inicjalizacja zgód i reklam")
                setupConsentAndLoadAd()
            }
        }
    }

    private fun changeNaviBarColor() {

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // 1. Wyłączamy jasny pasek nawigacji i włączamy ciemny
        controller.isAppearanceLightNavigationBars = false
        controller.isAppearanceLightStatusBars = false
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.BLACK
    }
    private fun setupConsentAndLoadAd() {
        Log.d(TAG, "Sprawdzanie zgód RODO...")


        val params = ConsentRequestParameters.Builder()

            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w(TAG, "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }

                    
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsAndLoad()
                    } else {
                        Log.d(TAG, "Brak zgody na reklamy - przechodzę do aplikacji")
                        navigateToMainApp()
                    }
                }
            },
            { requestConsentError ->
                
                Log.w(TAG, "${requestConsentError.errorCode}: ${requestConsentError.message}")
                
                navigateToMainApp()
            }
        )
    }

    private fun initializeMobileAdsAndLoad() {
        
        MobileAds.initialize(this) { }
        loadAppOpenAd()
    }
    

    private fun loadAppOpenAd() {
        Log.d(TAG, "Ładowanie reklamy...")
        val request = AdRequest.Builder().build()

        
        val timeoutHandler = android.os.Handler(mainLooper)
        val timeoutRunnable = Runnable {
            if (appOpenAd == null && !isDismissed) {
                Log.d(TAG, "Timeout ładowania reklamy")
                navigateToMainApp()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, AD_TIMEOUT_MS)

        AppOpenAd.load(
            this,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    Log.d(TAG, "Reklama załadowana")
                    appOpenAd = ad
                    showAdIfAvailable()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    Log.e(TAG, "Błąd ładowania reklamy: ${error.message}")
                    navigateToMainApp()
                }
            }
        )
    }

    private fun showAdIfAvailable() {
        val ad = appOpenAd

        // Korzystamy z nowej właściwości isPremiumValue z SubscriptionManager
        val isPremium = SubscriptionManager.getInstance(applicationContext).isPremiumValue

        if (isPremium || ad == null || isShowingAd) {
            Log.d(TAG, "Pomijam reklamę: Premium=$isPremium, Ad=$ad")
            navigateToMainApp()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Reklama App Open zamknięta")
                isShowingAd = false
                navigateToMainApp()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Błąd wyświetlania reklamy: ${error.message}")
                isShowingAd = false
                navigateToMainApp()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }

        isShowingAd = true
        ad.show(this)
    }

    private fun navigateToMainApp() {
        if (isDismissed) return
        isDismissed = true

        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}