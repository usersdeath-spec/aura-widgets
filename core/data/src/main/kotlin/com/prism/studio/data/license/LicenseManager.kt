package com.prism.studio.data.license

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import dagger.hilt.android.qualifiers.ApplicationContext
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.licenseStore by preferencesDataStore("license")

/**
 * One product, bought once, owned forever.
 *
 * Two decisions worth stating, because they shape how the app behaves offline:
 *
 * 1. Entitlement is cached in DataStore and treated as authoritative until Play explicitly says
 *    the purchase was refunded. Someone who bought the app on a plane keeps their widgets. A paid
 *    app that locks people out because Play Services is unreachable earns one-star reviews, and
 *    the piracy it prevents is not worth them.
 *
 * 2. There is no feature gating anywhere else in the codebase. Nothing checks [isUnlocked] except
 *    the store screen shown before purchase. Adding a gate later would mean touching one place,
 *    which is exactly the constraint that keeps the promise honest.
 */
@Singleton
class LicenseManager @Inject constructor(
    // Qualified deliberately: Hilt binds two Contexts with different lifetimes, and a
    // @Singleton holding an Activity context would leak every screen the user ever opened.
    @ApplicationContext private val context: Context,
) {
    private val unlockedKey = booleanPreferencesKey("unlocked_forever")

    val isUnlocked: Flow<Boolean> = context.licenseStore.data.map { it[unlockedKey] == true }

    private val connection = MutableStateFlow(false)

    private val billing: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases -> handle(result, purchases.orEmpty()) }
        .build()

    fun start() {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connection.value = result.responseCode == BillingClient.BillingResponseCode.OK
                if (connection.value) refresh()
            }

            override fun onBillingServiceDisconnected() {
                connection.value = false
                // Deliberately no retry storm: the cached entitlement already covers this case.
            }
        })
    }

    private fun refresh() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases -> handle(result, purchases) }
    }

    private fun handle(result: BillingResult, purchases: List<Purchase>) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        val owned = purchases.any {
            it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (owned) {
            purchases.filterNot { it.isAcknowledged }.forEach { acknowledge(it) }
            persist(true)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { /* Play retries for us if this fails. */ }
    }

    private fun persist(unlocked: Boolean) {
        kotlinx.coroutines.runBlocking {
            context.licenseStore.edit { it[unlockedKey] = unlocked }
        }
    }

    companion object {
        const val PRODUCT_ID = "prism_lifetime"
    }
}
