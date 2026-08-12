package com.miguelcode.warrantyvault.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pide el permiso del sistema cuando el usuario intenta activar el toggle.
    // Si lo concede → preferencia local a true. Si lo deniega → snackbar con CTA a Ajustes.
    // Se resuelven en composición porque showSnackbar corre fuera de ella. Estaban como
    // literales en español: un usuario con el móvil en inglés veía toda la app traducida
    // menos justo el mensaje que le explica cómo recuperar los avisos.
    val permissionDeniedMessage = stringResource(R.string.settings_permission_denied)
    val openSettingsLabel = stringResource(R.string.settings_open_settings)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = permissionDeniedMessage,
                    actionLabel = openSettingsLabel,
                    withDismissAction = true
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    val onToggleNotifications: (Boolean) -> Unit = { wantsEnabled ->
        if (!wantsEnabled) {
            viewModel.setNotificationsEnabled(false)
        } else {
            // Activar: comprobar permiso del sistema antes
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                viewModel.setNotificationsEnabled(true)
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.setNotificationsEnabled(true)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
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
                .padding(16.dp)
        ) {
            // Plan section
            Text(stringResource(R.string.settings_plan_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            val planLabel = if (isPremium) stringResource(R.string.settings_plan_premium) else stringResource(R.string.settings_plan_free)
            val planLimit = stringResource(R.string.settings_plan_limit)
            val planUpgrade = stringResource(R.string.settings_plan_upgrade)
            val planCardDescription = if (isPremium) planLabel else "$planLabel. $planLimit. $planUpgrade"
            // Card entera clickable cuando es Free → un único foco de TalkBack que lee
            // todo el contenido y dispara la navegación a Premium con doble toque.
            val planCard: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = planLabel, style = MaterialTheme.typography.titleMedium)
                        if (!isPremium) {
                            Text(
                                text = planLimit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    if (!isPremium) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = planUpgrade,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (isPremium) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) { planCard() }
            } else {
                Card(
                    onClick = onNavigateToPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            contentDescription = planCardDescription
                            role = Role.Button
                            onClick(label = null) { onNavigateToPremium(); true }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) { planCard() }
            }

            Spacer(Modifier.height(24.dp))

            // Appearance section
            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            label = { Text(stringResource(R.string.settings_theme_system)) }
                        )
                        FilterChip(
                            selected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            label = { Text(stringResource(R.string.settings_theme_light)) }
                        )
                        FilterChip(
                            selected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            label = { Text(stringResource(R.string.settings_theme_dark)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Notifications section
            Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            val remindersLabel = stringResource(R.string.settings_notifications_reminders)
            // Card entera toggleable: TalkBack la anuncia como un único interruptor que
            // lee la etiqueta y el estado on/off, y se alterna con doble toque desde
            // cualquier punto de la card (no solo desde el Switch).
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = notificationsEnabled,
                        onValueChange = onToggleNotifications,
                        role = Role.Switch
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(12.dp))
                    Text(remindersLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = null
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // App section
            Text(stringResource(R.string.settings_app), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        }
    }
}
