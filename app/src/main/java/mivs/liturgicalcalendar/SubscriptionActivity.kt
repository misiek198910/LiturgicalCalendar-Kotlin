package mivs.liturgicalcalendar

import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import mivs.liturgicalcalendar.billing.BillingManager
import mivs.liturgicalcalendar.billing.SubscriptionManager
import mivs.liturgicalcalendar.billing.SubscriptionStatus

class SubscriptionActivity : AppCompatActivity(), BillingManager.BillingManagerListener {

    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var tvStatus: TextView
    private lateinit var btnYearly: Button
    private lateinit var btnMonthly: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subscription)

        subscriptionManager = SubscriptionManager.getInstance(this)
        initUI()
        changeNaviBarColor();
        setupEdgeToEdge()
        observeData()
    }

    private fun changeNaviBarColor() {

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // 1. Wyłączamy jasny pasek nawigacji i włączamy ciemny
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
            }
            insets
        }
    }
    private fun initUI() {
        tvStatus = findViewById(R.id.subscription_status_text)
        btnYearly = findViewById(R.id.btn_subscribe_year)
        btnMonthly = findViewById(R.id.btn_subscribe_month)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Przycisk przywracania zakupów
        findViewById<Button>(R.id.restore_purchases_button).setOnClickListener {
            subscriptionManager.billingManager.queryPurchasesAsync()
            Toast.makeText(this, "Sprawdzanie aktywnych subskrypcji...", Toast.LENGTH_SHORT).show()
        }

        // Ustawienie listenera dla błędów/potwierdzeń
        subscriptionManager.billingManager.setListener(this)
    }
    private fun observeData() {
        subscriptionManager.productDetails.observe(this) { details ->
            if (details != null) {

                btnYearly.text = subscriptionManager.billingManager.getPlanOfferInfo(
                    this, details, BillingManager.BASE_PLAN_YEARLY
                )
                btnYearly.isSingleLine = false
                btnYearly.maxLines = 2


                btnMonthly.text = subscriptionManager.billingManager.getPlanOfferInfo(
                    this, details, BillingManager.BASE_PLAN_MONTHLY
                )

                btnYearly.setOnClickListener {
                    subscriptionManager.billingManager.launchPurchaseFlow(
                        this, details,BillingManager.BASE_PLAN_YEARLY
                    )
                }

                btnMonthly.setOnClickListener {
                    subscriptionManager.billingManager.launchPurchaseFlow(
                        this, details, BillingManager.BASE_PLAN_MONTHLY
                    )
                }
            }
        }

        subscriptionManager.subscriptionStatus.observe(this) { status ->
            when (status) {
                SubscriptionStatus.PREMIUM -> {
                    tvStatus.text = getString(R.string.subs_status_active)
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
                    btnYearly.isEnabled = false
                    btnMonthly.isEnabled = false
                }
                SubscriptionStatus.NON_PREMIUM -> {
                    tvStatus.text = getString(R.string.subs_status_none)
                    tvStatus.setTextColor(getColor(android.R.color.white))
                    btnYearly.isEnabled = true
                    btnMonthly.isEnabled = true
                }
                SubscriptionStatus.CHECKING -> {
                    tvStatus.text = getString(R.string.subs_status_checking)
                }
            }
        }
    }

    override fun onPurchaseAcknowledged() {
        runOnUiThread {
            Toast.makeText(this, "Dziękujemy za zakup! Reklamy zostały usunięte.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread {
            Toast.makeText(this, "Błąd: $error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionManager.billingManager.setListener(null)
    }
}