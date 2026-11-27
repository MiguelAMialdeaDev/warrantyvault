package com.miguelcode.warrantyvault.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import com.miguelcode.warrantyvault.ui.R as UiR
import com.miguelcode.warrantyvault.ui.common.AppLinks
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

    val restoring by viewModel.restoring.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }

    val restoredMessage = stringResource(R.string.settings_restore_done)
    val nothingFoundMessage = stringResource(R.string.settings_restore_nothing)
    LaunchedEffect(viewModel) {
        viewModel.restoreResults.collect { result ->
            snackbarHostState.showSnackbar(
                if (result == SettingsViewModel.RestoreResult.Restored) restoredMessage else nothingFoundMessage
            )
        }
    }

    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    // Los destinos y el origen los elige el usuario con el selector del sistema (Drive,
    // Descargas, un USB...): la app no necesita ningún permiso de almacenamiento.
    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it.toString()) } }
    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it.toString()) } }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { viewModel.exportPdf(it.toString()) } }

    val res = context.resources
    val backupFailed = stringResource(R.string.settings_backup_failed)
    val notABackup = stringResource(R.string.settings_backup_not_a_backup)
    LaunchedEffect(viewModel) {
        viewModel.backupResults.collect { result ->
            val message = when (result) {
                is SettingsViewModel.BackupResult.Exported ->
                    res.getQuantityString(R.plurals.settings_backup_exported, result.count, result.count)
                is SettingsViewModel.BackupResult.PdfExported ->
                    res.getQuantityString(R.plurals.settings_backup_pdf_done, result.count, result.count)
                is SettingsViewModel.BackupResult.Imported -> buildString {
                    val s = result.summary
                    append(res.getQuantityString(R.plurals.settings_backup_imported, s.imported, s.imported))
                    if (s.skippedDuplicates > 0) {
                        append(' ')
                        append(res.getQuantityString(R.plurals.settings_backup_duplicates, s.skippedDuplicates, s.skippedDuplicates))
                    }
                    if (s.archivedForLimit > 0) {
                        append(' ')
                        append(res.getQuantityString(R.plurals.settings_backup_archived_limit, s.archivedForLimit, s.archivedForLimit))
                    }
                }
                SettingsViewModel.BackupResult.NotABackup -> notABackup
                SettingsViewModel.BackupResult.Failed -> backupFailed
            }
            snackbarHostState.showSnackbar(message)
        }
    }

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

            // Backup section. La copia automática de Google no lleva las fotos de los
            // tickets: esto es lo único que las saca del móvil.
            Text(stringResource(R.string.settings_backup_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsLinkRow(
                        icon = Icons.Outlined.Backup,
                        label = stringResource(R.string.settings_backup_export),
                        enabled = !backupBusy,
                        onClick = { exportZipLauncher.launch(backupFileName("zip")) }
                    )
                    SettingsLinkRow(
                        icon = Icons.Outlined.Restore,
                        label = stringResource(R.string.settings_backup_import),
                        enabled = !backupBusy,
                        onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
                    )
                    SettingsLinkRow(
                        icon = Icons.Outlined.PictureAsPdf,
                        label = stringResource(
                            if (isPremium) R.string.settings_backup_pdf else R.string.settings_backup_pdf_premium
                        ),
                        enabled = !backupBusy,
                        // El PDF es Premium: sin suscripción lleva a la pantalla de planes.
                        onClick = {
                            if (isPremium) pdfLauncher.launch(backupFileName("pdf")) else onNavigateToPremium()
                        }
                    )
                    Text(
                        stringResource(R.string.settings_backup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // App section
            Text(stringResource(R.string.settings_app), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            val uriHandler = LocalUriHandler.current
            val privacyUrl = stringResource(UiR.string.privacy_policy_url)
            val contactEmail = stringResource(R.string.settings_contact_email)
            val contactSubject = stringResource(R.string.settings_contact_subject)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    if (isPremium) {
                        SettingsLinkRow(
                            icon = Icons.Outlined.CreditCard,
                            label = stringResource(R.string.settings_manage_subscription),
                            onClick = { uriHandler.openUri(AppLinks.manageSubscriptions(context.packageName)) }
                        )
                    } else {
                        SettingsLinkRow(
                            icon = Icons.Outlined.Restore,
                            label = stringResource(
                                if (restoring) R.string.settings_restoring else R.string.settings_restore_purchases
                            ),
                            enabled = !restoring,
                            onClick = viewModel::restorePurchases
                        )
                    }
                    SettingsLinkRow(
                        icon = Icons.Outlined.Policy,
                        label = stringResource(UiR.string.privacy_policy_label),
                        onClick = { uriHandler.openUri(privacyUrl) }
                    )
                    SettingsLinkRow(
                        icon = Icons.Outlined.Email,
                        label = stringResource(R.string.settings_contact),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(contactEmail))
                                putExtra(Intent.EXTRA_SUBJECT, "$contactSubject $versionName")
                            }
                            // Sin app de correo instalada startActivity lanzaría: mejor
                            // enseñar la dirección para que el usuario la copie.
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                scope.launch { snackbarHostState.showSnackbar(contactEmail) }
                            }
                        }
                    )
                    // La versión sale del paquete instalado: el texto fijo "1.0.0" seguía
                    // ahí en la 1.0.2 y despistaba al pedir soporte.
                    Text(
                        stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Nombre propuesto en el selector: warrantyvault-2026-11-27.zip */
private fun backupFileName(extension: String) = "warrantyvault-${java.time.LocalDate.now()}.$extension"
