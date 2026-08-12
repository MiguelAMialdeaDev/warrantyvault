package com.miguelcode.warrantyvault.feature.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.util.toPriceOrNull
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.usecase.AddWarrantyUseCase
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantyByIdUseCase
import com.miguelcode.warrantyvault.domain.usecase.UpdateWarrantyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AddEditViewModel(
    private val warrantyId: Long?,
    private val getWarrantyByIdUseCase: GetWarrantyByIdUseCase,
    private val addWarrantyUseCase: AddWarrantyUseCase,
    private val updateWarrantyUseCase: UpdateWarrantyUseCase,
    private val checkPremiumLimitUseCase: CheckPremiumLimitUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    init {
        if (warrantyId != null) {
            loadWarranty(warrantyId)
        } else {
            checkCanAdd()
        }
    }

    private fun loadWarranty(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getWarrantyByIdUseCase(id)) {
                is Result.Success -> {
                    val warranty = result.data
                    val totalMonths = ChronoUnit.MONTHS.between(warranty.purchaseDate, warranty.warrantyEndDate)
                    val showAsYears = totalMonths >= 12 && totalMonths % 12 == 0L
                    val displayAmount = if (showAsYears) totalMonths / 12 else totalMonths
                    _state.update {
                        it.copy(
                            isEditMode = true,
                            isLoading = false,
                            productName = warranty.productName,
                            category = warranty.category,
                            store = warranty.store,
                            price = warranty.price?.toString() ?: "",
                            purchaseDate = warranty.purchaseDate,
                            warrantyMonths = displayAmount.toString(),
                            isYears = showAsYears,
                            warrantyEndDate = warranty.warrantyEndDate,
                            warrantyMonthsError = null,
                            receiptPhotoPath = warranty.receiptPhotoPath,
                            notes = warranty.notes
                        )
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun checkCanAdd() {
        viewModelScope.launch {
            val canAdd = checkPremiumLimitUseCase()
            _state.update { it.copy(canAdd = canAdd) }
        }
    }

    fun onProductNameChanged(name: String) {
        // Trunca silenciosamente si excede el máximo (mejor UX que ignorar nuevos
        // caracteres). El TextField muestra un contador para que el usuario lo vea.
        val capped = if (name.length > MAX_PRODUCT_NAME_LENGTH) {
            name.take(MAX_PRODUCT_NAME_LENGTH)
        } else name
        _state.update { it.copy(productName = capped, productNameError = null) }
    }

    fun onCategoryChanged(category: WarrantyCategory) {
        _state.update { it.copy(category = category) }
    }

    fun onStoreChanged(store: String) {
        _state.update { it.copy(store = store) }
    }

    fun onPriceChanged(price: String) {
        _state.update { it.copy(price = price) }
    }

    fun onPurchaseDateChanged(date: LocalDate) {
        _state.update { it.copy(purchaseDate = date, purchaseDateError = null) }
        validateAndRecalculate()
    }

    fun onWarrantyMonthsChanged(months: String) {
        // Limita la longitud antes de procesar para evitar overflow de Long.
        val trimmed = months.take(MAX_INPUT_DIGITS).filter { it.isDigit() }
        _state.update { it.copy(warrantyMonths = trimmed) }
        validateAndRecalculate()
    }

    fun onIsYearsChanged(isYears: Boolean) {
        _state.update { it.copy(isYears = isYears) }
        validateAndRecalculate()
    }

    fun onPhotoChanged(path: String?) {
        _state.update { it.copy(receiptPhotoPath = path) }
    }

    fun onNotesChanged(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    private fun validateAndRecalculate() {
        val state = _state.value
        val raw = state.warrantyMonths
        val parsed = raw.toLongOrNull()
        val totalMonths = parsed?.let { if (state.isYears) it * 12 else it }

        // Calcula el error independientemente de si hay fecha — así el usuario lo ve
        // aunque aún no haya elegido fecha de compra.
        val error: AddEditError? = when {
            raw.isBlank() -> null
            parsed == null -> AddEditError.InvalidNumber
            parsed <= 0 -> AddEditError.MustBePositive
            totalMonths != null && totalMonths > MAX_WARRANTY_MONTHS ->
                if (state.isYears) AddEditError.MaxYears else AddEditError.MaxMonths
            else -> null
        }

        val newEndDate = if (
            error == null &&
            state.purchaseDate != null &&
            totalMonths != null &&
            totalMonths in 1..MAX_WARRANTY_MONTHS
        ) {
            runCatching { state.purchaseDate.plusMonths(totalMonths) }.getOrNull()
        } else {
            null
        }

        _state.update { it.copy(warrantyMonthsError = error, warrantyEndDate = newEndDate) }
    }

    fun onSave() {
        val current = _state.value
        // Doble guarda: isSaving bloquea double-tap durante la inserción, isSaved
        // bloquea re-inserciones tras un guardado exitoso (por si la navegación
        // fuera aún no ha ocurrido).
        if (current.isSaving || current.isSaved) return

        if (current.productName.isBlank()) {
            _state.update { it.copy(productNameError = AddEditError.NameRequired) }
            return
        }
        if (current.purchaseDate == null) {
            _state.update { it.copy(purchaseDateError = AddEditError.DateRequired) }
            return
        }
        if (current.warrantyEndDate == null) {
            _state.update { it.copy(warrantyMonthsError = AddEditError.DurationRequired) }
            return
        }
        if (!current.isEditMode && !current.canAdd) {
            _state.update { it.copy(formError = AddEditError.PremiumLimit) }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val warranty = Warranty(
                id = warrantyId ?: 0,
                productName = current.productName.trim(),
                category = current.category,
                store = current.store.trim(),
                price = current.price.toPriceOrNull(),
                purchaseDate = current.purchaseDate,
                warrantyEndDate = current.warrantyEndDate,
                receiptPhotoPath = current.receiptPhotoPath,
                notes = current.notes.trim()
            )

            val result = if (current.isEditMode) {
                updateWarrantyUseCase(warranty)
            } else {
                addWarrantyUseCase(warranty)
            }

            when (result) {
                is Result.Success -> _state.update { it.copy(isSaving = false, isSaved = true) }
                is Result.Error -> _state.update {
                    it.copy(isSaving = false, error = result.exception.message)
                }
            }
        }
    }

    /**
     * Los consume la pantalla tras mostrarlos. Sin esto el error se quedaría en el estado
     * y el snackbar no volvería a salir si el usuario repite la misma acción fallida.
     */
    fun onErrorShown() {
        _state.update { it.copy(error = null) }
    }

    fun onFormErrorShown() {
        _state.update { it.copy(formError = null) }
    }

    companion object {
        private const val MAX_WARRANTY_MONTHS = 600L // 50 años
        private const val MAX_INPUT_DIGITS = 4 // 9999 cabe sobrado en Long
        const val MAX_PRODUCT_NAME_LENGTH = 60
    }
}
