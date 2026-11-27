package com.miguelcode.warrantyvault.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.miguelcode.warrantyvault.BuildConfig
import com.miguelcode.warrantyvault.R
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract.Companion.STATE_ERROR
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract.Companion.STATE_IDLE
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract.Companion.STATE_LOADING
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract.Companion.STATE_PURCHASED
import com.miguelcode.warrantyvault.domain.billing.SubscriptionPrice
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(
    context: Context,
    private val preferencesRepository: PreferencesRepository
) : BillingManagerContract {

    private val appContext = context.applicationContext

    private val verifier = PurchaseVerifier(BuildConfig.PLAY_LICENSE_KEY)

    private fun isAuthentic(purchase: Purchase): Boolean =
        verifier.isAuthentic(purchase.originalJson, purchase.signature)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _stateCode = MutableStateFlow(STATE_IDLE)
    override val stateCode: StateFlow<Int> = _stateCode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _prices = MutableStateFlow<Map<String, SubscriptionPrice>>(emptyMap())
    override val prices: StateFlow<Map<String, SubscriptionPrice>> = _prices.asStateFlow()

    private val _plansUnavailable = MutableStateFlow(false)
    override val plansUnavailable: StateFlow<Boolean> = _plansUnavailable.asStateFlow()

    private val products = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())

    /** Evita lanzar dos startConnection a la vez desde connect() y desde el backoff. */
    private var connecting = false
    private var reconnectAttempts = 0
    private var productQueryAttempts = 0

    private val purchasesListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _stateCode.value = STATE_IDLE
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // El usuario ya tiene la suscripción pero el flag local dice que no
                // (reinstalación, móvil nuevo). No es un error: hay que restaurarla.
                refreshPurchases()
            }
            else -> {
                _errorMessage.value = userMessageFor(billingResult)
                _stateCode.value = STATE_ERROR
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesListener)
        // Desde Billing 7 hay que declarar explícitamente el soporte de compras
        // pendientes. `enableOneTimeProducts()` no es opcional: sin él, el build() de
        // PendingPurchasesParams lanza IllegalArgumentException. No se activan los planes
        // prepago porque la app solo vende suscripciones recurrentes.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override fun connect() {
        if (billingClient.isReady) {
            // Ya conectados: aprovechamos la llamada para revalidar contra Play.
            refreshPurchases()
            return
        }
        if (connecting) return
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    scope.launch {
                        queryProducts()
                        checkExistingPurchases()
                    }
                } else {
                    // Dispositivo sin Play Services, país no soportado, servicio caído…
                    // Sin cuenta de Google en Play, BILLING_UNAVAILABLE no se arregla
                    // reintentando: la pantalla Premium se quedaba en "Cargando planes"
                    // para siempre. Se avisa ya y se sigue reintentando por si acaso.
                    if (result.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                        _plansUnavailable.value = true
                    }
                    scheduleReconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                scheduleReconnect()
            }
        })
    }

    /**
     * Reconexión con backoff exponencial y tope. La versión anterior llamaba a connect()
     * directamente desde onBillingServiceDisconnected: si Play no estaba disponible eso
     * era un bucle cerrado que se comía la batería sin llegar a conectar nunca.
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            // Sin conexión tras todos los intentos: que la pantalla deje de esperar.
            if (_prices.value.isEmpty()) _plansUnavailable.value = true
            return
        }
        val delayMs = RECONNECT_BASE_DELAY_MS shl reconnectAttempts
        reconnectAttempts++
        scope.launch {
            delay(delayMs)
            connect()
        }
    }

    override fun refreshPurchases() {
        if (!billingClient.isReady) {
            // connect() ya consulta las compras en cuanto la conexión se establece.
            connect()
            return
        }
        scope.launch {
            // Segunda vía de recuperación: si la consulta de productos falló al conectar,
            // volver a primer plano la reintenta en vez de arrastrar la sesión sin precios.
            if (products.value.isEmpty()) {
                productQueryAttempts = 0
                queryProducts()
            }
            checkExistingPurchases()
        }
    }

    override fun clearError() {
        _errorMessage.value = null
        if (_stateCode.value == STATE_ERROR) _stateCode.value = STATE_IDLE
    }

    private suspend fun queryProducts() {
        val productIds = listOf(
            BillingManagerContract.PRODUCT_MONTHLY,
            BillingManagerContract.PRODUCT_YEARLY
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()

        val result = suspendCancellableCoroutine { cont ->
            // Billing 8 devuelve un QueryProductDetailsResult: los productos que Play no
            // encuentra van aparte (unfetchedProductList) en vez de desaparecer sin más.
            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                cont.resume(
                    Triple(billingResult, queryResult.productDetailsList, queryResult.unfetchedProductList)
                )
            }
        }

        if (result.first.responseCode == BillingClient.BillingResponseCode.OK) {
            val details = result.second.associateBy { it.productId }
            products.value = details
            _prices.value = details.mapNotNull { (id, product) ->
                product.recurringPrice()?.let { id to it }
            }.toMap()
            productQueryAttempts = 0
            // Un producto que Play no encuentra no es un fallo de red: reintentar no lo
            // arregla. Se avisa una vez en vez de dejar la pantalla esperando.
            _plansUnavailable.value = _prices.value.isEmpty()
            if (result.third.isNotEmpty()) {
                _errorMessage.value = appContext.getString(R.string.billing_error_product_unavailable)
            }
        } else if (productQueryAttempts < MAX_PRODUCT_QUERY_ATTEMPTS) {
            // Sin reintento, un fallo de red puntual dejaba la pantalla Premium sin
            // precios el resto de la sesión: el botón de compra se quedaba deshabilitado
            // en "cargando" sin error ni forma de recuperarse.
            val delayMs = PRODUCT_QUERY_BASE_DELAY_MS shl productQueryAttempts
            productQueryAttempts++
            delay(delayMs)
            queryProducts()
        } else if (_prices.value.isEmpty()) {
            _plansUnavailable.value = true
        }
    }

    /**
     * Precio recurrente real del plan. Cogemos la última fase del ciclo porque las
     * anteriores pueden ser prueba gratuita u oferta de entrada: enseñar la primera
     * mostraría "0 €" como precio del plan.
     */
    private fun ProductDetails.recurringPrice(): SubscriptionPrice? {
        val phases = subscriptionOfferDetails
            ?.let(BillingRules::basePlanOffer)
            ?.pricingPhases
            ?.pricingPhaseList
            ?: return null
        val phase = BillingRules.recurringPhase(phases) ?: return null
        return SubscriptionPrice(
            formatted = phase.formattedPrice,
            amountMicros = phase.priceAmountMicros
        )
    }

    private suspend fun checkExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
                cont.resume(billingResult to purchasesList)
            }
        }

        // Si la consulta falla no tocamos nada: preferimos mantener el estado anterior
        // antes que revocarle premium a alguien por un fallo de red o de servicio.
        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) return

        // Reintenta el acknowledge de compras que quedaron a medias en sesiones
        // anteriores. Play reembolsa automáticamente a las 72 h toda compra que nunca
        // se confirma, así que este barrido en cada arranque es la última red de seguridad.
        BillingRules.needingAcknowledgement(result.second, appContext.packageName, ::isAuthentic).forEach { acknowledgeWithRetry(it) }

        // Aquí está la revocación: si Play ya no devuelve la suscripción (cancelada,
        // caducada, reembolsada) el flag local baja a false. Y si la devuelve tras
        // reinstalar o cambiar de móvil, se restaura sola.
        val hasPremium = BillingRules.hasPremium(result.second, appContext.packageName, ::isAuthentic)
        preferencesRepository.setIsPremium(hasPremium)
        _stateCode.value = if (hasPremium) STATE_PURCHASED else STATE_IDLE
    }

    override fun launchPurchase(activity: Activity, productId: String) {
        val productDetails = products.value[productId] ?: run {
            _errorMessage.value = appContext.getString(R.string.billing_error_product_unavailable)
            _stateCode.value = STATE_ERROR
            return
        }
        val offerToken = productDetails.subscriptionOfferDetails
            ?.let(BillingRules::purchaseOffer)
            ?.offerToken
            ?: run {
                _errorMessage.value = appContext.getString(R.string.billing_error_product_unavailable)
                _stateCode.value = STATE_ERROR
                return
            }

        _stateCode.value = STATE_LOADING

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        // Si Play no llega a abrir la hoja de compra (conexión caída, servicio no
        // disponible…) el listener nunca se llama. Sin comprobar el resultado, el botón
        // se quedaba en "cargando" hasta salir y volver a la app.
        val launchResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _errorMessage.value = userMessageFor(launchResult)
            _stateCode.value = STATE_ERROR
            if (launchResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                connect()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Mismo filtro que la revalidación: una compra de otro paquete o de un
                // producto que no es premium no se confirma ni da premium.
                if (BillingRules.activePurchases(listOf(purchase), appContext.packageName, ::isAuthentic).isEmpty()) {
                    return
                }
                if (purchase.isAcknowledged) {
                    grantPremium()
                } else {
                    acknowledgeWithRetry(purchase)
                }
            }
            Purchase.PurchaseState.PENDING -> {
                // Pago diferido (efectivo, transferencia). Todavía no hay dinero, así que
                // no damos premium: Play avisará por el listener cuando se complete.
                _stateCode.value = STATE_IDLE
                _errorMessage.value = appContext.getString(R.string.billing_purchase_pending)
            }
            else -> Unit
        }
    }

    /**
     * Confirmar la compra es obligatorio: si Play no recibe el acknowledge en 3 días
     * revierte el cobro y el usuario pierde el premium que pagó. Un solo intento sin
     * rama de error, como estaba antes, convertía cualquier fallo puntual de red en un
     * reembolso silencioso.
     */
    private fun acknowledgeWithRetry(purchase: Purchase) {
        scope.launch {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            repeat(ACK_MAX_ATTEMPTS) { attempt ->
                val result = suspendCancellableCoroutine { cont ->
                    billingClient.acknowledgePurchase(params) { cont.resume(it) }
                }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    grantPremium()
                    return@launch
                }
                // Backoff derivado del intento, igual que en la reconexión. Con un `var`
                // acumulador el compilador avisaba de que no se leía.
                if (attempt < ACK_MAX_ATTEMPTS - 1) {
                    delay(ACK_BASE_DELAY_MS shl attempt)
                }
            }

            // Agotados los reintentos de esta sesión. El usuario ha pagado, así que le
            // damos premium igualmente; checkExistingPurchases() lo reintentará en cada
            // arranque mientras quede dentro de la ventana de 3 días.
            grantPremium()
        }
    }

    private fun grantPremium() {
        scope.launch { preferencesRepository.setIsPremium(true) }
        _stateCode.value = STATE_PURCHASED
    }

    /**
     * `debugMessage` viene de Play en inglés y orientado a desarrollador. Nunca debe
     * llegar a la pantalla: lo traducimos a un mensaje que el usuario entienda.
     */
    private fun userMessageFor(result: BillingResult): String {
        val resId = when (result.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                R.string.billing_error_network
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                R.string.billing_error_unavailable
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                R.string.billing_error_product_unavailable
            else -> R.string.billing_error_generic
        }
        return appContext.getString(resId)
    }

    fun destroy() {
        scope.cancel()
        billingClient.endConnection()
    }

    private companion object {
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val RECONNECT_BASE_DELAY_MS = 1_000L
        const val MAX_PRODUCT_QUERY_ATTEMPTS = 3
        const val PRODUCT_QUERY_BASE_DELAY_MS = 1_000L
        const val ACK_MAX_ATTEMPTS = 4
        const val ACK_BASE_DELAY_MS = 1_000L
    }
}
