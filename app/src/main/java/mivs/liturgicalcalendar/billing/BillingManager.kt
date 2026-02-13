package mivs.liturgicalcalendar.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import mivs.liturgicalcalendar.R
import mivs.liturgicalcalendar.data.db.AppDatabase
import mivs.liturgicalcalendar.data.entity.UserStatusEntity
import kotlinx.coroutines.*
import androidx.core.graphics.toColorInt

class BillingManager private constructor(context: Context) {
    private val billingClient: BillingClient
    private val database = AppDatabase.getDatabase(context.applicationContext)
    private val dao = database.userStatusDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    private val _subscriptionStatus = MutableLiveData(SubscriptionStatus.CHECKING)
    val subscriptionStatus: LiveData<SubscriptionStatus> = _subscriptionStatus

    private val _productDetails = MutableLiveData<ProductDetails?>()
    val productDetails: LiveData<ProductDetails?> = _productDetails
    private val appContext = context.applicationContext

    interface BillingManagerListener {
        fun onPurchaseAcknowledged()
        fun onPurchaseError(error: String?)
    }

    private var listener: BillingManagerListener? = null
    fun setListener(listener: BillingManagerListener?) { this.listener = listener }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) { handlePurchase(purchase) }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {

            listener?.onPurchaseError(appContext.getString(R.string.billing_error_canceled))
        } else {

            listener?.onPurchaseError(appContext.getString(R.string.billing_error_generic, billingResult.responseCode.toString()))
        }
    }

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        connectToGooglePlay()

        scope.launch {
            val status = dao.getStatus()
            val isFull = status?.isPremium ?: false
            _isPremium.postValue(isFull)
            _subscriptionStatus.postValue(if (isFull) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    queryPurchasesAsync()
                    queryProductDetails()
                } else {
                    _subscriptionStatus.postValue(SubscriptionStatus.NON_PREMIUM)
                }
            }
            override fun onBillingServiceDisconnected() { connectToGooglePlay() }
        })
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var hasPremium = false
                var token: String? = null
                purchases.forEach { purchase ->
                    if (purchase.products.contains(SKU_REMOVE_ADS_YEAR) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasPremium = true
                        token = purchase.purchaseToken
                        if (!purchase.isAcknowledged) handlePurchase(purchase)
                    }
                }
                updateLocalStatus(hasPremium, token)
            }
        }
    }

    private fun updateLocalStatus(hasPremium: Boolean, token: String?) {
        scope.launch {
            dao.insert(UserStatusEntity(isPremium = hasPremium, purchaseToken = token))
            _isPremium.postValue(hasPremium)
            _subscriptionStatus.postValue(if (hasPremium) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_REMOVE_ADS_YEAR)
                .setProductType(ProductType.SUBS)
                .build()
        )
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(productList).build()) { _, details ->
            if (details.isNotEmpty()) _productDetails.postValue(details[0])
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetailsToPurchase: ProductDetails, basePlanId: String) {

        val offers = productDetailsToPurchase.subscriptionOfferDetails?.filter {
            it.basePlanId == basePlanId
        }

        val trialOffer = offers?.find { offer ->
            offer.pricingPhases.pricingPhaseList.any { phase -> phase.priceAmountMicros == 0L }
        }

        val selectedOffer = trialOffer ?: offers?.firstOrNull()

        if (selectedOffer == null) {
            listener?.onPurchaseError(appContext.getString(R.string.billing_error_offer_not_found, basePlanId))
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetailsToPurchase)
                .setOfferToken(selectedOffer.offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingResponseCode.OK) {
                    updateLocalStatus(true, purchase.purchaseToken)
                    listener?.onPurchaseAcknowledged()
                }
            }
        } else if (purchase.isAcknowledged) {
            updateLocalStatus(true, purchase.purchaseToken)
        }
    }

    fun getPlanOfferInfo(context: Context, productDetails: ProductDetails?, basePlanId: String): CharSequence {

        val offers = productDetails?.subscriptionOfferDetails

        val offerWithTrial = offers?.find {
            it.basePlanId == basePlanId && it.pricingPhases.pricingPhaseList.any { phase -> phase.priceAmountMicros == 0L }
        }

        val finalOffer = offerWithTrial ?: offers?.find { it.basePlanId == basePlanId }

        val trialPhase = finalOffer?.pricingPhases?.pricingPhaseList?.find { it.priceAmountMicros == 0L }
        val basePhase = finalOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()

        val price = basePhase?.formattedPrice ?: return context.getString(R.string.subs_buy_button)

        return when (basePlanId) {
            BASE_PLAN_YEARLY -> {
                if (trialPhase != null) {
                    val fullText = context.getString(R.string.subs_trial_button, price)
                    val spannable = android.text.SpannableString(fullText)
                    val newLineIndex = fullText.indexOf("\n")
                    if (newLineIndex != -1) {
                        spannable.setSpan(android.text.style.RelativeSizeSpan(0.75f), newLineIndex, fullText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(android.text.style.ForegroundColorSpan("#BDBDBD".toColorInt()), newLineIndex, fullText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    spannable
                } else {
                    context.getString(R.string.subs_annual_button, price)
                }
            }
            BASE_PLAN_MONTHLY -> context.getString(R.string.subs_monthly_button, price)
            else -> price
        }
    }

    companion object {
        @Volatile private var INSTANCE: BillingManager? = null
        fun getInstance(context: Context): BillingManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
        }

        const val SKU_REMOVE_ADS_YEAR = "remove_ads_for_year"

        const val BASE_PLAN_YEARLY = "kalendarz-liturgiczny-year"
        const val BASE_PLAN_MONTHLY = "kalendarz-liturgiczny-month"

    }
}