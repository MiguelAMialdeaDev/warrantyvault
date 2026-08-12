package com.miguelcode.warrantyvault.feature.addedit

import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import java.time.LocalDate
import java.time.LocalDateTime

enum class AddEditError {
    NameRequired,
    DateRequired,
    DurationRequired,
    MaxMonths,
    MaxYears,
    InvalidNumber,
    MustBePositive,
    PremiumLimit
}

data class AddEditState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val productName: String = "",
    val productNameError: AddEditError? = null,
    val category: WarrantyCategory = WarrantyCategory.ELECTRONICS,
    val store: String = "",
    val price: String = "",
    val purchaseDate: LocalDate? = null,
    val purchaseDateError: AddEditError? = null,
    val warrantyMonths: String = "",
    val warrantyMonthsError: AddEditError? = null,
    val isYears: Boolean = false,
    val warrantyEndDate: LocalDate? = null,
    val receiptPhotoPath: String? = null,
    /** La copia de la foto al almacenamiento interno está en curso. */
    val isPhotoLoading: Boolean = false,
    val notes: String = "",
    /**
     * Fecha de creación original al editar. Null en alta: `onSave` la sella entonces.
     * Sin conservarla, el @Update de Room la sobrescribía con la fecha actual en cada
     * edición y el dato original se perdía sin posible recuperación.
     */
    val createdAt: LocalDateTime? = null,
    /** Error genérico (ej. fallo del use case). Se muestra crudo, no se localiza. */
    val error: String? = null,
    /** Error de formulario localizable (ej. límite premium alcanzado). */
    val formError: AddEditError? = null,
    val canAdd: Boolean = true
) {
    val isFormValid: Boolean
        get() = productName.isNotBlank() &&
                purchaseDate != null &&
                warrantyEndDate != null &&
                warrantyMonthsError == null
}
