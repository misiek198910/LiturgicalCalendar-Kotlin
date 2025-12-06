package mivs.liturgicalcalendar.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.* // DODANO
import mivs.liturgicalcalendar.data.db.AppDatabase // DODANO
import mivs.liturgicalcalendar.data.entity.UserStatusEntity // DODANO

class BillingManager private constructor(context: Context) {
    private val billingClient: BillingClient

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium
    private val database = AppDatabase.getDatabase(context.applicationContext)
    private val dao = database.userStatusDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    interface BillingManagerListener {
        fun onPurchaseAcknowledged()
        fun onPurchaseError(error: String?)
    }

    private var listener: BillingManagerListener? = null

    fun setListener(listener: BillingManagerListener?) {
        this.listener = listener
    }

    // Status subskrypcji
    private val _subscriptionStatus = MutableLiveData<SubscriptionStatus>(SubscriptionStatus.CHECKING)
    val subscriptionStatus: LiveData<SubscriptionStatus> = _subscriptionStatus

    // Szczegóły produktu
    private val _productDetails = MutableLiveData<ProductDetails?>()
    val productDetails: LiveData<ProductDetails?> = _productDetails

    // Aktywna subskrypcja
    private val _activeSubscription = MutableLiveData<Purchase?>()
    val activeSubscription: LiveData<Purchase?> = _activeSubscription

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            listener?.onPurchaseError("Anulowano zakup.")
        } else {
            listener?.onPurchaseError("Błąd zakupu. Kod: ${billingResult.responseCode}")
            Log.e(TAG, "Błąd zakupu: ${billingResult.debugMessage}")
        }
    }

    init {
        _subscriptionStatus.value = SubscriptionStatus.CHECKING
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        connectToGooglePlay()

        // KROK 2: ŁADOWANIE STANU Z DB PRZY STARTCIE (SZYBKI ODCZYT LOKALNY)
        scope.launch {
            val status = dao.getStatus()
            _isPremium.postValue(status?.isPremium ?: false)
            _subscriptionStatus.postValue(if (status?.isPremium == true) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d(TAG, "Połączono z usługą płatności Google Play.")
                    queryPurchasesAsync()
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Nie udało się połączyć z usługą płatności. Kod: ${billingResult.responseCode}")
                    _subscriptionStatus.postValue(SubscriptionStatus.NON_PREMIUM)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Rozłączono z usługą płatności. Próba ponownego połączenia...")
                connectToGooglePlay()
            }
        })
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var hasPremium = false
                var activeSub: Purchase? = null

                purchases.forEach { purchase ->
                    if (purchase.products.contains(SKU_REMOVE_ADS) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        activeSub = purchase
                        hasPremium = true
                        if (!purchase.isAcknowledged) {
                            handlePurchase(purchase) // Potwierdzi i zapisze do DB
                        }
                        return@forEach
                    }
                }

                // KROK 3: ZAPISYWANIE STANU PO ODCZYCIE Z GOOGLE (Stan końcowy)
                scope.launch {
                    val status = UserStatusEntity(isPremium = hasPremium, purchaseToken = activeSub?.purchaseToken)
                    dao.insert(status)
                    _isPremium.postValue(hasPremium)
                    _activeSubscription.postValue(activeSub)
                    _subscriptionStatus.postValue(if (hasPremium) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
                }
            } else {
                // KROK 4: W PRZYPADKU BŁĘDU ONLINE, ŁADUJEMY STAN Z DB (Fast Fail)
                scope.launch {
                    val status = dao.getStatus()
                    _isPremium.postValue(status?.isPremium ?: false)
                }
            }
        }
    }

    fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_REMOVE_ADS)
                .setProductType(ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                _productDetails.postValue(productDetailsList[0])
                Log.d(TAG, "Pobrano szczegóły produktu: ${productDetailsList[0].name}")
            } else {
                Log.e(TAG, "Nie udało się pobrać szczegółów produktu. Błąd: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetailsToPurchase: ProductDetails) {
        if (!billingClient.isReady) {
            Log.e(TAG, "Klient płatności niegotowy.")
            return
        }

        val offerDetails = productDetailsToPurchase.subscriptionOfferDetails?.firstOrNull() ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetailsToPurchase)
                .setOfferToken(offerDetails.offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.d(TAG, "Zakup potwierdzony.")
                        // ZAPIS DO BAZY PO POTWIERDZENIU
                        scope.launch {
                            val status = UserStatusEntity(isPremium = true, purchaseToken = purchase.purchaseToken)
                            dao.insert(status)
                            _isPremium.postValue(true) // Aktualizujemy UI
                        }
                        listener?.onPurchaseAcknowledged()
                    } // ... (błędy) ...
                }
            } else {
                Log.d(TAG, "Zakup był już potwierdzony.")
                // ZAPIS DO BAZY (W przypadku powrotu do aplikacji po długim czasie)
                scope.launch {
                    val status =
                        UserStatusEntity(isPremium = true, purchaseToken = purchase.purchaseToken)
                    dao.insert(status)
                    _isPremium.postValue(true)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val TAG = "BillingManager"
        // TUTAJ WPISUJEMY TWÓJ NOWY IDENTYFIKATOR:
        const val SKU_REMOVE_ADS: String = "remove_ads_for_year"
    }
}