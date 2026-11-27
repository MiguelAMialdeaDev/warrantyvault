package com.miguelcode.warrantyvault.feature.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.feature.premium.R
import com.miguelcode.warrantyvault.ui.R as UiR
import com.miguelcode.warrantyvault.ui.common.AppLinks
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    viewModel: PremiumViewModel = koinViewModel()
) {
    val billingState by viewModel.stateCode.collectAsStateWithLifecycle()
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val plansUnavailable by viewModel.plansUnavailable.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val annualSavings by viewModel.annualSavingsPercent.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val alreadyPremium = isPremium || billingState == BillingManagerContract.STATE_PURCHASED

    // LocalContext.current devuelve un ContextWrapper (tema, ViewModelStore…), no
    // necesariamente la Activity. Recorremos la cadena hasta encontrarla, porque
    // BillingClient.launchBillingFlow requiere Activity, no Context.
    val activity = LocalContext.current.findActivity()

    val snackbarHostState = remember { SnackbarHostState() }
    // Sin esto los fallos de compra eran invisibles: el estado se quedaba en ERROR y el
    // usuario solo veía que no pasaba nada.
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onErrorShown()
    }

    // Sin precios de Play no se puede comprar: launchBillingFlow necesita ProductDetails,
    // así que dejar el botón activo solo llevaría a un error evitable.
    val plansReady = prices.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.premium_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.premium_compare), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            // La comparativa lista únicamente lo que la app hace hoy. Lo que está a medias
            // vive en la sección "En camino", fuera de lo que se paga.
            ComparisonHeader()
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ComparisonRow(
                feature = stringResource(R.string.premium_feature_warranties),
                free = stringResource(R.string.premium_free_limit),
                premium = stringResource(R.string.premium_unlimited)
            )
            ComparisonRow(
                feature = stringResource(R.string.premium_feature_notifications),
                free = stringResource(R.string.premium_notifications_detail),
                premium = stringResource(R.string.premium_notifications_detail)
            )
            ComparisonRow(
                feature = stringResource(R.string.premium_feature_photo),
                free = stringResource(R.string.premium_included),
                premium = stringResource(R.string.premium_included)
            )

            Spacer(Modifier.height(32.dp))

            var selectedPlan by rememberSaveable { mutableStateOf(PLAN_ANNUAL) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PriceCard(
                    title = stringResource(R.string.premium_monthly),
                    price = prices[BillingManagerContract.PRODUCT_MONTHLY]?.formatted
                        ?: stringResource(R.string.premium_price_placeholder),
                    period = stringResource(R.string.premium_monthly_period),
                    highlighted = selectedPlan == PLAN_MONTHLY,
                    onClick = { selectedPlan = PLAN_MONTHLY },
                    modifier = Modifier.weight(1f)
                )
                PriceCard(
                    title = stringResource(R.string.premium_annual),
                    price = prices[BillingManagerContract.PRODUCT_YEARLY]?.formatted
                        ?: stringResource(R.string.premium_price_placeholder),
                    period = stringResource(R.string.premium_annual_period),
                    // La insignia solo aparece si el ahorro es real según los precios de
                    // Play. Un "Ahorra 67%" fijo mentiría en cuanto cambies precios o el
                    // usuario esté en otro país.
                    badge = annualSavings?.let { stringResource(R.string.premium_save_badge, it) },
                    highlighted = selectedPlan == PLAN_ANNUAL,
                    onClick = { selectedPlan = PLAN_ANNUAL },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val productId = if (selectedPlan == PLAN_MONTHLY)
                        BillingManagerContract.PRODUCT_MONTHLY
                    else
                        BillingManagerContract.PRODUCT_YEARLY
                    activity?.let { viewModel.launchPurchase(it, productId) }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = activity != null &&
                        plansReady &&
                        !alreadyPremium &&
                        billingState != BillingManagerContract.STATE_LOADING
            ) {
                when {
                    billingState == BillingManagerContract.STATE_LOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    alreadyPremium -> Text(stringResource(R.string.premium_cta_purchased))
                    !plansReady && plansUnavailable -> Text(stringResource(R.string.premium_cta_unavailable))
                    !plansReady -> Text(stringResource(R.string.premium_cta_loading))
                    else -> Text(stringResource(R.string.premium_cta))
                }
            }

            Spacer(Modifier.height(8.dp))
            val uriHandler = LocalUriHandler.current
            if (alreadyPremium) {
                // Quien ya paga necesita una salida clara para cambiar de plan o cancelar.
                // Play exige que sea fácil, y sin enlace había que buscarlo en Play Store.
                val packageName = LocalContext.current.packageName
                TextButton(onClick = { uriHandler.openUri(AppLinks.manageSubscriptions(packageName)) }) {
                    Text(stringResource(R.string.premium_manage_subscription))
                }
            } else {
                // La política de suscripciones de Play pide decir, junto al botón de compra,
                // cuánto se cobra, cada cuánto, que se renueva sola y cómo cancelar. Con
                // solo "Cancela cuando quieras" la ficha se arriesgaba a un rechazo.
                val selectedPrice = prices[
                    if (selectedPlan == PLAN_MONTHLY) BillingManagerContract.PRODUCT_MONTHLY
                    else BillingManagerContract.PRODUCT_YEARLY
                ]?.formatted
                val disclosure = when {
                    selectedPrice == null -> stringResource(R.string.premium_renewal_disclosure_generic)
                    selectedPlan == PLAN_MONTHLY -> stringResource(R.string.premium_renewal_disclosure_monthly, selectedPrice)
                    else -> stringResource(R.string.premium_renewal_disclosure_yearly, selectedPrice)
                }
                if (!plansReady && plansUnavailable) {
                    Text(
                        stringResource(R.string.premium_unavailable_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    disclosure,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            val privacyUrl = stringResource(UiR.string.privacy_policy_url)
            TextButton(onClick = { uriHandler.openUri(privacyUrl) }) {
                Text(stringResource(UiR.string.privacy_policy_label))
            }

            Spacer(Modifier.height(32.dp))
            RoadmapSection()
        }
    }
}

@Composable
private fun ComparisonHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.premium_column_free),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.premium_column_premium),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ComparisonRow(feature: String, free: String, premium: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(feature, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(free, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(premium, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoadmapSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.premium_roadmap_title),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.premium_roadmap_pdf),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                stringResource(R.string.premium_roadmap_backup),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.premium_roadmap_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PriceCard(
    title: String,
    price: String,
    period: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(price, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
            Text(period, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

private const val PLAN_MONTHLY = "monthly"
private const val PLAN_ANNUAL = "annual"

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
