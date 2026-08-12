package com.miguelcode.warrantyvault.feature.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miguelcode.warrantyvault.feature.addedit.components.CategoryChips
import com.miguelcode.warrantyvault.feature.addedit.components.DatePickerField
import com.miguelcode.warrantyvault.feature.addedit.components.PhotoPickerBox
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.format.DateTimeFormatter

/** Resuelve un error tipado a string localizable. */
@Composable
private fun AddEditError.localized(): String = stringResource(
    when (this) {
        AddEditError.NameRequired -> R.string.addedit_error_name_required
        AddEditError.DateRequired -> R.string.addedit_error_date_required
        AddEditError.DurationRequired -> R.string.addedit_error_duration_required
        AddEditError.MaxMonths -> R.string.addedit_error_max_months
        AddEditError.MaxYears -> R.string.addedit_error_max_years
        AddEditError.InvalidNumber -> R.string.addedit_error_invalid_number
        AddEditError.MustBePositive -> R.string.addedit_error_must_positive
        AddEditError.PremiumLimit -> R.string.addedit_error_premium_limit
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    onNavigateBack: () -> Unit,
    warrantyId: Long? = null,
    viewModel: AddEditViewModel = koinViewModel { parametersOf(warrantyId) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Cierra el teclado y commitea el valor en composición del IME antes de guardar.
    // Sin esto, si el usuario pulsa Guardar mientras escribe, el último carácter
    // se pierde porque el TextField aún no había emitido onValueChange.
    val onSaveSafe: () -> Unit = {
        focusManager.clearFocus()
        viewModel.onSave()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val savedPath = PhotoStorage.copyToInternal(context, it)
            if (savedPath != null) viewModel.onPhotoChanged(savedPath)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Sin esto, un guardado fallido era invisible: el ViewModel dejaba el error en el
    // estado, la pantalla no lo leía, y el usuario veía que el botón simplemente no hacía
    // nada. `formError` es localizable (límite premium) y `error` viene del use case.
    val snackbarHostState = remember { SnackbarHostState() }

    // El string se resuelve en composición (localized() es @Composable) y el efecto solo
    // lo muestra, que es lo que permite reutilizar el mapeo ya existente arriba.
    val formErrorText = state.formError?.localized()
    LaunchedEffect(formErrorText) {
        val message = formErrorText ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onFormErrorShown()
    }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onErrorShown()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditMode) R.string.addedit_title_edit else R.string.addedit_title_new
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.addedit_nav_close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSaveSafe,
                        enabled = state.isFormValid && !state.isSaving
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = stringResource(R.string.addedit_nav_save),
                            tint = if (state.isFormValid) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Photo
            PhotoPickerBox(
                photoPath = state.receiptPhotoPath,
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // Product name
            OutlinedTextField(
                value = state.productName,
                onValueChange = viewModel::onProductNameChanged,
                label = { Text(stringResource(R.string.addedit_field_product_name)) },
                leadingIcon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null) },
                isError = state.productNameError != null,
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.productNameError?.localized() ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${state.productName.length}/${AddEditViewModel.MAX_PRODUCT_NAME_LENGTH}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Category
            CategoryChips(
                selected = state.category,
                onCategorySelected = viewModel::onCategoryChanged
            )

            // Store
            OutlinedTextField(
                value = state.store,
                onValueChange = viewModel::onStoreChanged,
                label = { Text(stringResource(R.string.addedit_field_store)) },
                leadingIcon = { Icon(Icons.Outlined.Store, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Price
            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChanged,
                label = { Text(stringResource(R.string.addedit_field_price)) },
                leadingIcon = { Text("€", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Purchase date
            DatePickerField(
                date = state.purchaseDate,
                onDateSelected = viewModel::onPurchaseDateChanged,
                label = stringResource(R.string.addedit_field_purchase_date),
                error = state.purchaseDateError?.localized(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Duration row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.warrantyMonths,
                    onValueChange = viewModel::onWarrantyMonthsChanged,
                    label = { Text(stringResource(R.string.addedit_field_duration)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.warrantyMonthsError != null,
                    supportingText = state.warrantyMonthsError?.let { { Text(it.localized()) } },
                    singleLine = true,
                    modifier = Modifier.weight(0.5f)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .weight(0.5f)
                        .padding(top = 8.dp)
                        .defaultMinSize(minHeight = 56.dp)
                ) {
                    SegmentedButton(
                        selected = !state.isYears,
                        onClick = { viewModel.onIsYearsChanged(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.addedit_segment_months), modifier = Modifier.padding(horizontal = 4.dp)) }
                    SegmentedButton(
                        selected = state.isYears,
                        onClick = { viewModel.onIsYearsChanged(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.addedit_segment_years), modifier = Modifier.padding(horizontal = 4.dp)) }
                }
            }

            // Calculated end date
            if (state.warrantyEndDate != null) {
                val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                Text(
                    text = stringResource(R.string.addedit_end_date_label, state.warrantyEndDate!!.format(fmt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                )
            }

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text(stringResource(R.string.addedit_field_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Indicador inline mientras guarda (el botón Guardar vive en el TopBar ✓)
            if (state.isSaving) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.addedit_saving),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
