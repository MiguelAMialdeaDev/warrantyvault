package com.miguelcode.warrantyvault.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.feature.detail.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelcode.warrantyvault.ui.common.ConfirmDeleteDialog
import com.miguelcode.warrantyvault.feature.detail.components.InfoCard
import com.miguelcode.warrantyvault.feature.detail.components.StatusSection
import com.miguelcode.warrantyvault.feature.detail.components.WarrantyHero
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    warrantyId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(warrantyId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    if (showDeleteDialog && state.warranty != null) {
        ConfirmDeleteDialog(
            productName = state.warranty!!.productName,
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.warranty?.productName ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    state.warranty?.let { warranty ->
                        IconButton(onClick = { onNavigateToEdit(warranty.id) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.detail_action_edit),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.detail_action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    // Nunca el mensaje de la excepción: puede llevar SQL o rutas internas.
                    Text(stringResource(R.string.detail_error_generic), color = MaterialTheme.colorScheme.error)
                }
            }
            state.warranty != null -> {
                val warranty = state.warranty!!
                // Column + verticalScroll en lugar de LazyColumn: la pantalla tiene
                // pocas secciones fijas y así garantizamos scroll completo cuando las
                // notas son largas (el LazyColumn con un único item gigante no siempre
                // mide bien la altura). El Spacer final + navigationBarsPadding evita
                // que la última línea quede tapada por la barra de navegación.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    WarrantyHero(
                        category = warranty.category,
                        photoPath = warranty.receiptPhotoPath
                    )
                    StatusSection(
                        warranty = warranty,
                        modifier = Modifier.offset(y = (-24).dp)
                    )
                    InfoCard(warranty = warranty)
                    Spacer(
                        modifier = Modifier
                            .height(32.dp)
                            .navigationBarsPadding()
                    )
                }
            }
            // Sin esta rama, un id inexistente dejaba la pantalla vacía: ni título, ni
            // mensaje, ni acciones, y solo se salía con el botón atrás.
            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.detail_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
