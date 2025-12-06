package mivs.liturgicalcalendar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.launch
import mivs.liturgicalcalendar.billing.SubscriptionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        // ID Twojej reklamy App Open (produkcyjne lub testowe ca-app-pub-3940256099942544/9257395921)
        private const val AD_UNIT_ID = "ca-app-pub-8612826840770530/9888347667"
        private const val AD_TIMEOUT_MS = 8000L // Wydłużamy czas na RODO + Reklamę
    }

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Sprawdzamy subskrypcję
        val subManager = SubscriptionManager.getInstance(applicationContext)
        subManager.billingManager.queryPurchasesAsync()

        lifecycleScope.launch {
            // Czekamy chwilę, by mieć pewność co do statusu subskrypcji (opcjonalne małe opóźnienie)
            // delay(500)

            if (subManager.billingManager.activeSubscription.value != null) {
                Log.d(TAG, "Użytkownik Premium - pomijam RODO i reklamy")
                navigateToMainApp()
            } else {
                // Użytkownik FREE - najpierw RODO, potem reklama
                setupConsentAndLoadAd()
            }
        }
    }

    // --- LOGIKA RODO (UMP SDK) ---
    private fun setupConsentAndLoadAd() {
        Log.d(TAG, "Sprawdzanie zgód RODO...")

        // Opcjonalnie: Ustawienia debugowania (tylko dla emulatora/urządzeń testowych)
        // Aby wymusić wyświetlenie okna, potrzebujesz ID urządzenia z Logcata
        /*
        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("TU_WPISZ_ID_Z_LOGCATA_JEŚLI_TESTUJESZ")
            .build()
        */

        val params = ConsentRequestParameters.Builder()
            // .setConsentDebugSettings(debugSettings) // Odkomentuj do testów
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)

        // 1. Pobierz status zgody
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // 2. Po pomyślnym pobraniu, załaduj formularz (jeśli wymagany)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        // Błąd formularza zgody - logujemy i idziemy dalej (próbujemy załadować reklamy lub przejść do apki)
                        Log.w(TAG, "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }

                    // 3. Sprawdź czy możemy wyświetlać reklamy
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsAndLoad()
                    } else {
                        Log.d(TAG, "Brak zgody na reklamy - przechodzę do aplikacji")
                        navigateToMainApp()
                    }
                }
            },
            { requestConsentError ->
                // Błąd pobierania statusu zgody
                Log.w(TAG, "${requestConsentError.errorCode}: ${requestConsentError.message}")
                // Próbujemy załadować mimo to (lub idziemy do apki)
                navigateToMainApp()
            }
        )
    }

    private fun initializeMobileAdsAndLoad() {
        // Dopiero TERAZ inicjalizujemy AdMob
        MobileAds.initialize(this) { }
        loadAppOpenAd()
    }
    // --- KONIEC LOGIKI RODO ---

    private fun loadAppOpenAd() {
        Log.d(TAG, "Ładowanie reklamy...")
        val request = AdRequest.Builder().build()

        // Zabezpieczenie czasowe
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
        // Jeszcze raz sprawdzamy Premium (na wszelki wypadek)
        val isPremium = SubscriptionManager.getInstance(applicationContext).billingManager.activeSubscription.value != null

        if (isPremium || ad == null || isShowingAd) {
            navigateToMainApp()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Reklama zamknięta")
                isShowingAd = false
                navigateToMainApp()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Błąd wyświetlania: ${error.message}")
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