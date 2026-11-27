package com.miguelcode.warrantyvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.review.ReviewPrompter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.navigation.AppNavGraph
import com.miguelcode.warrantyvault.navigation.Screen
import com.miguelcode.warrantyvault.notifications.NotificationEntry
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: PreferencesRepository by inject()
    private val billingManager: BillingManagerContract by inject()
    private val syncExpirationAlerts: SyncExpirationAlertsUseCase by inject()
    private val warrantyRepository: WarrantyRepository by inject()
    private val reviewPrompter: ReviewPrompter by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Sincroniza la preferencia con la decisión real del usuario.
        lifecycleScope.launch {
            preferencesRepository.setNotificationsEnabled(granted)
            // El sync del arranque ya corrió con la preferencia a false y no programó nada.
            // Sin esto, quien restaura garantías de una copia y concede el permiso se quedaba
            // sin avisos hasta el siguiente arranque en frío.
            syncExpirationAlerts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super.onCreate: después, la ventana ya se ha creado con el tema del
        // manifiesto y el cambio no evita el fotograma del color equivocado.
        applyForcedWindowTheme()
        // La app no recibe nada de fuera: ni deep links ni extras. Pero NavController lee
        // del intent de arranque sus extras internos (deepLinkIds, deepLinkArgs) y otra app
        // podía usarlos para abrir directamente cualquier pantalla, por ejemplo la de
        // editar con un id elegido. Se vacían antes de montar el grafo.
        // La única excepción es el id que trae el aviso de caducidad, y solo si llega por
        // el alias no exportado (ver NotificationEntry). Al recrear la actividad no se
        // vuelve a usar: NavController ya restaura su propia pila.
        if (savedInstanceState == null) {
            pendingWarrantyId.value = NotificationEntry.warrantyIdFrom(intent)
        }
        intent = sanitized(intent)
        super.onCreate(savedInstanceState)
        // La miniatura de Recientes guardaba la última pantalla: el detalle con precio,
        // notas y la foto del ticket, que puede llevar dirección o los últimos dígitos de
        // la tarjeta. Las capturas que haga el usuario siguen funcionando (no es
        // FLAG_SECURE): solo se oculta la miniatura. Antes de Android 13 no existe.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
        enableEdgeToEdge()
        // Sincroniza la preferencia con el estado real del permiso del sistema en cada
        // creación. Si el usuario revoca/concede en Ajustes, la preferencia local se ajusta.
        syncNotificationPreferenceWithSystem()
        setContent {
            val themeMode by preferencesRepository.themeModeFlow
                .collectAsState(initial = ThemeModeCache.MODE_SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeModeCache.MODE_DARK -> true
                ThemeModeCache.MODE_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            // Deja el modo elegido donde el próximo arranque en frío pueda leerlo sin
            // esperar a DataStore, que es lo que causaba el destello.
            LaunchedEffect(themeMode) { ThemeModeCache.write(this@MainActivity, themeMode) }

            // enableEdgeToEdge() a secas decide el color de los iconos de las barras con el
            // tema del sistema: con "Oscuro" forzado y el sistema en claro, la hora y la
            // batería salían en negro sobre el fondo oscuro de la app.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT, Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        NAV_BAR_LIGHT_SCRIM, NAV_BAR_DARK_SCRIM
                    ) { darkTheme }
                )
                onDispose { }
            }

            WarrantyVaultTheme(darkTheme = darkTheme) {
                // initial = null: hasta que DataStore emite el valor real, mostramos un splash
                // vacío. Así evitamos pintar Home y luego saltar a Onboarding (flash).
                val onboardingDone by preferencesRepository.isOnboardingDoneFlow
                    .collectAsState(initial = null)

                if (onboardingDone == null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) { Box(Modifier.fillMaxSize()) }
                } else {
                    // El permiso se pide con el onboarding ya terminado, cuando la app ya ha
                    // explicado para qué son los avisos. Antes salía encima de la primera
                    // pantalla, sin contexto, y mucha gente lo denegaba por reflejo. Una vez
                    // por creación de la actividad: rememberSaveable evita repetirlo al rotar.
                    var permissionAsked by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(onboardingDone) {
                        if (onboardingDone == true && !permissionAsked) {
                            permissionAsked = true
                            requestNotificationPermissionIfNeeded()
                        }
                    }
                    // Valoración tras guardar una garantía (y tener al menos dos): es cuando la
                    // app acaba de ser útil. El primer valor solo fija la referencia.
                    LaunchedEffect(Unit) {
                        var previous: Int? = null
                        warrantyRepository.getAllWarranties()
                            .map { it.size }
                            .distinctUntilChanged()
                            .collect { count ->
                                if (ReviewPrompter.isSuccessMoment(previous, count)) {
                                    reviewPrompter.maybeAsk(this@MainActivity)
                                }
                                previous = count
                            }
                    }
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController, onboardingDone = onboardingDone!!)
                    val warrantyToOpen by pendingWarrantyId
                    LaunchedEffect(warrantyToOpen, onboardingDone) {
                        val id = warrantyToOpen ?: return@LaunchedEffect
                        // Sin onboarding hecho no hay garantías que abrir: se descarta.
                        if (onboardingDone == true) {
                            navController.navigate(Screen.Detail.createRoute(id)) {
                                launchSingleTop = true
                            }
                        }
                        pendingWarrantyId.value = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        NotificationEntry.warrantyIdFrom(intent)?.let { pendingWarrantyId.value = it }
        super.onNewIntent(sanitized(intent))
    }

    /** Garantía que el aviso pidió abrir y que el grafo aún no ha mostrado. */
    private val pendingWarrantyId = mutableStateOf<Long?>(null)

    /** Copia del intent sin extras ni datos, conservando acción, categorías y flags. */
    private fun sanitized(source: Intent): Intent = Intent(source).apply {
        replaceExtras(Bundle())
        data = null
    }

    override fun onResume() {
        super.onResume()
        // El usuario puede cancelar o resucitar la suscripción desde la app de Google
        // Play sin pasar por la nuestra. Al volver a primer plano revalidamos, que es
        // el único momento fiable para enterarnos.
        billingManager.refreshPurchases()
    }

    /**
     * Con el modo "sistema" no se toca nada: de eso ya se encarga `values-night`. Solo
     * cuando el usuario ha forzado claro u oscuro desde Ajustes hace falta sustituir el
     * tema de ventana, porque el recurso por defecto sigue al sistema y no a la app.
     */
    private fun applyForcedWindowTheme() {
        when (ThemeModeCache.read(this)) {
            ThemeModeCache.MODE_DARK -> setTheme(R.style.Theme_WarrantyVault_Forced_Dark)
            ThemeModeCache.MODE_LIGHT -> setTheme(R.style.Theme_WarrantyVault_Forced_Light)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun syncNotificationPreferenceWithSystem() {
        // < API 33: no hay permiso runtime, no hay nada que sincronizar.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        // Solo bajamos a false cuando el sistema NO ha concedido. Nunca subimos a true
        // automáticamente: el usuario tiene que activarlo explícitamente en Settings o
        // conceder el permiso desde el dialog del sistema. Así evitamos pisar la
        // preferencia local del usuario que ha apagado el toggle a propósito.
        if (!granted) {
            lifecycleScope.launch { preferencesRepository.setNotificationsEnabled(false) }
        }
    }

    private companion object {
        // Los mismos scrims que usa enableEdgeToEdge() por defecto para la barra de
        // navegación con botones.
        val NAV_BAR_LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val NAV_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
