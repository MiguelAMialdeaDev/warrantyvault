package com.miguelcode.warrantyvault.feature.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.ui.common.EmptyStateView
import com.miguelcode.warrantyvault.feature.home.components.FilterChipsRow
import com.miguelcode.warrantyvault.feature.home.components.PremiumBanner
import com.miguelcode.warrantyvault.feature.home.components.StatsRow
import com.miguelcode.warrantyvault.feature.home.components.WarrantyCard
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

/** Tarjetas que entran escalonadas: aproximadamente las que caben en pantalla. */
private const val STAGGERED_ITEMS = 6
private const val STAGGER_STEP_MS = 40L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        onWarrantyClick = onNavigateToDetail,
        onAddClick = onNavigateToAdd,
        onSettingsClick = onNavigateToSettings,
        onPremiumClick = onNavigateToPremium,
        onFilterSelected = viewModel::onFilterSelected,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onRetry = viewModel::retry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    onWarrantyClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onFilterSelected: (WarrantyFilter) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    /** Tarjetas que ya hicieron su animación de entrada; no se repite al reciclarlas. */
    val animatedIds = remember { mutableSetOf<Long>() }
    // rememberSaveable: con `remember`, al rotar volvía a false y el efecto de abajo
    // borraba la query que sí había sobrevivido en el ViewModel. El usuario perdía la
    // búsqueda entera a media escritura.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // La query se limpia en la acción explícita de cerrar la búsqueda, no aquí: reaccionar
    // al valor inicial tras una recreación es justo lo que la borraba sin querer.
    LaunchedEffect(searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
        }
    }
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_app_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchActive) {
                            onSearchQueryChanged("")
                        }
                        searchActive = !searchActive
                    }) {
                        Icon(
                            imageVector = if (searchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = stringResource(
                                if (searchActive) R.string.home_action_close_search else R.string.home_action_search
                            ),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.home_action_settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (state.limitReached) {
                // Free con 3+ garantías: el FAB invita a Premium en lugar de abrir Add.
                ExtendedFloatingActionButton(
                    onClick = onPremiumClick,
                    expanded = expandedFab,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Stars,
                            // Plegado solo queda el icono: sin etiqueta, TalkBack lo anunciaba como "botón" a secas.
                            contentDescription = if (expandedFab) null else stringResource(R.string.home_fab_premium)
                        )
                    },
                    text = { Text(stringResource(R.string.home_fab_premium)) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    expanded = expandedFab,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = if (expandedFab) null else stringResource(R.string.home_fab_add)
                        )
                    },
                    text = { Text(stringResource(R.string.home_fab_add)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.loadError) {
            EmptyStateView(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.home_error_title),
                subtitle = stringResource(R.string.home_error_subtitle),
                actionLabel = stringResource(R.string.home_error_retry),
                onAction = onRetry,
                modifier = Modifier.padding(padding)
            )
        } else if (!state.isLoading && state.warranties.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_empty_subtitle),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onAddClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stats Row
                item(key = "stats") {
                    StatsRow(
                        activeCount = state.activeCount,
                        expiringCount = state.expiringCount,
                        expiredCount = state.expiredCount,
                        onActiveClick = { onFilterSelected(WarrantyFilter.ACTIVE) },
                        onExpiringClick = { onFilterSelected(WarrantyFilter.EXPIRING_SOON) },
                        onExpiredClick = { onFilterSelected(WarrantyFilter.EXPIRED) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Filter Chips
                item(key = "filters") {
                    FilterChipsRow(
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = onFilterSelected,
                        showArchived = state.hasArchived,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Premium Banner
                if (state.showPremiumBanner) {
                    item(key = "premium_banner") {
                        PremiumBanner(onUpgradeClick = onPremiumClick)
                    }
                }

                // Warranty Cards with staggered animation
                itemsIndexed(
                    items = state.filteredWarranties,
                    key = { _, warranty -> warranty.id }
                ) { index, warranty ->
                    // El escalonado se aplica solo a las tarjetas del primer viewport y
                    // solo la primera vez que aparecen. Con el índice absoluto y
                    // LaunchedEffect(Unit), una tarjeta profunda esperaba su delay
                    // completo cada vez que volvía a pantalla, y mientras tanto
                    // AnimatedVisibility no ocupaba altura: la lista daba saltos al
                    // hacer scroll y las tarjetas tardaban segundos en verse.
                    val staggered = index < STAGGERED_ITEMS && warranty.id !in animatedIds
                    var visible by remember(warranty.id) { mutableStateOf(!staggered) }
                    LaunchedEffect(warranty.id) {
                        if (!visible) {
                            delay(index * STAGGER_STEP_MS)
                            visible = true
                        }
                        animatedIds += warranty.id
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(200)) + slideInVertically(
                            initialOffsetY = { 60 },
                            animationSpec = tween(200)
                        )
                    ) {
                        WarrantyCard(
                            warranty = warranty,
                            onClick = { onWarrantyClick(warranty.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Empty filter state
                if (state.filteredWarranties.isEmpty() && state.warranties.isNotEmpty()) {
                    item(key = "empty_filter") {
                        EmptyStateView(
                            icon = Icons.Outlined.Search,
                            title = stringResource(R.string.home_no_results_title),
                            subtitle = stringResource(R.string.home_no_results_subtitle),
                            modifier = Modifier.height(300.dp)
                        )
                    }
                }
            }
        }
    }
}

// Previews

@Preview(name = "Light - Content", showBackground = true)
@Preview(name = "Dark - Content", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentPreview() {
    WarrantyVaultTheme {
        HomeScreenContent(
            state = HomeState(
                isLoading = false,
                warranties = sampleWarranties(),
                usedSlots = 3,
                isPremium = false
            ),
            onWarrantyClick = {},
            onAddClick = {},
            onSettingsClick = {},
            onPremiumClick = {},
            onFilterSelected = {}
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    WarrantyVaultTheme {
        HomeScreenContent(
            state = HomeState(isLoading = false),
            onWarrantyClick = {},
            onAddClick = {},
            onSettingsClick = {},
            onPremiumClick = {},
            onFilterSelected = {}
        )
    }
}

private fun sampleWarranties() = listOf(
    Warranty(
        id = 1,
        productName = "MacBook Pro 14\"",
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        price = 2499.0,
        purchaseDate = LocalDate.now().minusMonths(6),
        warrantyEndDate = LocalDate.now().plusMonths(18)
    ),
    Warranty(
        id = 2,
        productName = "PlayStation 5",
        category = WarrantyCategory.GAMING,
        store = "MediaMarkt",
        purchaseDate = LocalDate.now().minusYears(1).minusMonths(11),
        warrantyEndDate = LocalDate.now().plusDays(15)
    ),
    Warranty(
        id = 3,
        productName = "Lavadora Samsung",
        category = WarrantyCategory.APPLIANCE,
        store = "El Corte Inglés",
        price = 599.0,
        purchaseDate = LocalDate.now().minusYears(3),
        warrantyEndDate = LocalDate.now().minusDays(30)
    )
)
