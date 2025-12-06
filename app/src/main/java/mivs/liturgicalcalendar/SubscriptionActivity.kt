package mivs.liturgicalcalendar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import mivs.liturgicalcalendar.billing.BillingManager
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.R

class SubscriptionActivity : AppCompatActivity(), BillingManager.BillingManagerListener {

    private var billingManager: BillingManager? = null
    private lateinit var statusTextView: TextView
    private lateinit var buyButton: Button
    private lateinit var restoreButton: Button
    private lateinit var btnback: ImageButton
    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subscription)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicjalizacja AdMob
        MobileAds.initialize(this) {}

        setupViews()
        setupBilling()
    }

    private fun setupViews() {
        statusTextView = findViewById(R.id.subscription_status_text)
        buyButton = findViewById(R.id.buy_subscription_button)
        restoreButton = findViewById(R.id.restore_purchases_button)
        btnback = findViewById(R.id.btnBack)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        btnback.setOnClickListener {
            finish()
        }

        buyButton.setOnClickListener {
            handleBuyButtonClick()
        }

        restoreButton.setOnClickListener {
            billingManager?.queryPurchasesAsync()
            Toast.makeText(this, "Sprawdzam status zakupów...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBilling() {
        // Pobieramy instancję BillingManager z singletona SubscriptionManager
        billingManager = SubscriptionManager.getInstance(applicationContext).billingManager
        billingManager?.setListener(this)

        // 1. GŁÓWNA OBSERWACJA: Status Premium (Boolean)
        billingManager?.isPremium?.observe(this) { hasPremium ->
            updateUI(hasPremium)
            handleAds(hasPremium) // Sterowanie reklamami na podstawie stanu Boolean
        }

        // 2. OBSERWACJA SZCZEGÓŁÓW PRODUKTU (ceny)
        billingManager?.productDetails?.observe(this) { details ->
            // Pobieramy aktualny stan PREMIUM z LiveData (jest on synchronizowany przez DB)
            val hasPremium = billingManager?.isPremium?.value ?: false

            if (!hasPremium && details != null) {
                // Użytkownik FREE - ładujemy cenę
                val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                val price = pricingPhase?.formattedPrice ?: "..."

                buyButton.text = "Kup subskrypcję ($price / rok)"
                buyButton.isEnabled = true
            } else if (hasPremium) {
                // Użytkownik PREMIUM - pokazujemy zarządzanie
                buyButton.text = "Zarządzaj subskrypcją"
                buyButton.isEnabled = true
            } else {
                // Stan oczekiwania na dane
                buyButton.text = "Ładowanie ceny..."
                buyButton.isEnabled = false
            }
        }

        // Wymuszamy odświeżenie danych przy starcie
        billingManager?.queryPurchasesAsync()
        billingManager?.queryProductDetails()
    }

    private fun updateUI(hasSubscription: Boolean) {
        if (hasSubscription) {
            statusTextView.text = "Status: Subskrypcja Aktywna"
            statusTextView.setTextColor(getColor(R.color.green_premium)) // Użyj koloru zielonego
            // Ustawiamy przycisk na zarządzanie (cena ustawi się w obserwatorze productDetails)
            buyButton.text = "Zarządzaj subskrypcją"
        } else {
            statusTextView.text = "Status: Brak aktywnej subskrypcji"
            statusTextView.setTextColor(getColor(R.color.gray_non_premium)) // Użyj koloru szarego

            // Jeśli cena jest już załadowana, przycisk pokaże cenę (ustawione w obserwatorze productDetails)
        }
    }

    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            // Użytkownik ma Premium -> Ukrywamy baner i zwalniamy zasoby
            if (adView != null) {
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            }
            adContainerLayout.visibility = View.GONE
        } else {
            // Użytkownik FREE -> Pokazujemy baner
            if (adView == null) {
                adContainerLayout.visibility = View.VISIBLE
                loadBannerAd()
            }
        }
    }

    private fun loadBannerAd() {

        val adBannerId: String = BuildConfig.AD_BANNER_ID

        if (adBannerId.contains("BRAK_ID") || adBannerId.isEmpty()) {
            Log.e("AD_MOB", "BŁĄD: AD_BANNER_ID jest puste lub nieprawidłowe.")
            adContainerLayout.visibility = View.GONE
            return
        }
        val adView = AdView(this)
        adView.adUnitId = adBannerId
        adView.setAdSize(adSize)
        this.adView = adView

        adContainer.removeAllViews()
        adContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density
            var adWidthPixels = adContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun handleBuyButtonClick() {
        // Używamy .value, bo jest to jednorazowy odczyt stanu przed akcją
        val activeSubscription = billingManager?.activeSubscription?.value

        if (activeSubscription != null) {
            // Otwieramy zarządzanie subskrypcjami w Google Play
            val sku = BillingManager.SKU_REMOVE_ADS
            val url = "https://play.google.com/store/account/subscriptions?sku=$sku&package=$packageName"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Nie można otworzyć sklepu Google Play", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Uruchamiamy proces zakupu
            val productDetails = billingManager?.productDetails?.value
            if (productDetails != null) {
                billingManager?.launchPurchaseFlow(this, productDetails)
            } else {
                Toast.makeText(this, "Jeszcze chwila, ładuję dane o produkcie...", Toast.LENGTH_SHORT).show()
                billingManager?.queryProductDetails()
            }
        }
    }

    override fun onPurchaseAcknowledged() {
        runOnUiThread {
            Toast.makeText(this, "Dziękujemy! Subskrypcja została aktywowana.", Toast.LENGTH_LONG).show()
            billingManager?.queryPurchasesAsync()
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread {
            Toast.makeText(this, "Błąd: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
        billingManager?.queryPurchasesAsync()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
        billingManager?.setListener(null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}