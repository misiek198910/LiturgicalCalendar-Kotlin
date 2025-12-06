package mivs.liturgicalcalendar.billing

import android.content.Context
import androidx.lifecycle.LiveData

class SubscriptionManager private constructor(context: Context) {

    // Inicjalizujemy BillingManager
    val billingManager: BillingManager = BillingManager.getInstance(context)

    // Wystawiamy status subskrypcji na zewnątrz
    val subscriptionStatus: LiveData<SubscriptionStatus> = billingManager.subscriptionStatus

    // Wystawiamy szczegóły produktu (np. cenę do wyświetlenia)
    val productDetails = billingManager.productDetails

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SubscriptionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}