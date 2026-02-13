package mivs.liturgicalcalendar.billing

import android.content.Context
import androidx.lifecycle.LiveData
import com.android.billingclient.api.ProductDetails

class SubscriptionManager private constructor(context: Context) {
    
    val billingManager: BillingManager = BillingManager.getInstance(context)

    val isPremium: LiveData<Boolean> = billingManager.isPremium
    val subscriptionStatus: LiveData<SubscriptionStatus> = billingManager.subscriptionStatus
    val productDetails: LiveData<ProductDetails?> = billingManager.productDetails
    val isPremiumValue: Boolean
        get() = billingManager.isPremium.value ?: false

    fun getFormattedPrice(context: Context, productDetails: ProductDetails?, planOrOfferId: String): CharSequence {
        return billingManager.getPlanOfferInfo(context, productDetails, planOrOfferId)
    }

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: SubscriptionManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}