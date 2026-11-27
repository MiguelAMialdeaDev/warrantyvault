package com.miguelcode.warrantyvault.feature.addedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.util.toPriceInput
import com.miguelcode.warrantyvault.domain.util.toPriceOrNull
import com.miguelcode.warrantyvault.domain.util.warrantyMonths
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.usecase.AddWarrantyUseCase
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantyByIdUseCase
import com.miguelcode.warrantyvault.domain.usecase.PremiumLimitReachedException
import com.miguelcode.warrantyvault.domain.usecase.UpdateWarrantyUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class AddEditViewModel(
    private val warrantyId: Long?,
    private val getWarrantyByIdUseCase: GetWarrantyByIdUseCase,
    private val addWarrantyUseCase: AddWarrantyUseCase,
    private val updateWarrantyUseCase: UpdateWarrantyUseCase,
    private val checkPremiumLimitUseCase: CheckPremiumLimitUseCase,
    /**
     * Opcional a propósito: si el entorno no puede aportarlo (tests), el formulario sigue
     * funcionando y solo se pierde la restauración tras muerte del proceso.
     */
    private val savedStateHandle: SavedStateHandle? = null,
    /** Where the receipt photo is copied. Injected so tests can run the copy deterministically. */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Fecha de hoy. Inyectable para que los tests no dependan del reloj. */
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    init {
        val draft = restoreDraft()
        when {
            // Elegir la foto del ticket saca al usuario de la app, que es justo cuando un
            // móvil con poca RAM mata el proceso. Con el formulario solo en memoria, al
            // volver estaba todo en blanco.
            draft != null -> {
                _state.value = draft
                validateAndRecalculate()
                if (warrantyId == null) checkCanAdd()
            }
            warrantyId != null -> loadWarranty(warrantyId)
            else -> {
                applyNewWarrantyDefaults()
                checkCanAdd()
            }
        }
    }

    /** Único punto de escritura del estado: cada cambio se respalda en el handle. */
    private fun updateState(transform: (AddEditState) -> AddEditState) {
        saveDraft(_state.updateAndGet(transform))
    }

    private fun loadWarranty(id: Long) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            when (val result = getWarrantyByIdUseCase(id)) {
                is Result.Success -> {
                    val warranty = result.data
                    val totalMonths = warrantyMonths(warranty.purchaseDate, warranty.warrantyEndDate)
                    val showAsYears = totalMonths >= 12 && totalMonths % 12 == 0L
                    val displayAmount = if (showAsYears) totalMonths / 12 else totalMonths
                    updateState {
                        it.copy(
                            isEditMode = true,
                            isLoading = false,
                            productName = warranty.productName,
                            category = warranty.category,
                            store = warranty.store,
                            price = warranty.price?.toPriceInput() ?: "",
                            purchaseDate = warranty.purchaseDate,
                            warrantyMonths = displayAmount.toString(),
                            isYears = showAsYears,
                            warrantyEndDate = warranty.warrantyEndDate,
                            warrantyMonthsError = null,
                            receiptPhotoPath = warranty.receiptPhotoPath,
                            notes = warranty.notes,
                            createdAt = warranty.createdAt
                        )
                    }
                }
                // La garantía ya no existe (borrada en otra pantalla, id inventado): se cierra
                // la pantalla. Antes se quedaba como un alta vacía que, al guardar, insertaba
                // con el id pedido.
                is Result.Error -> updateState {
                    it.copy(isLoading = false, notFound = true)
                }
            }
        }
    }

    /**
     * Un alta arranca con la compra de hoy y la garantía legal de España (3 años desde
     * 2022). Es lo más habitual al registrar algo recién comprado, y antes los dos campos
     * empezaban vacíos y Guardar seguía desactivado sin decir por qué.
     */
    private fun applyNewWarrantyDefaults() {
        updateState {
            it.copy(
                purchaseDate = today(),
                warrantyMonths = DEFAULT_WARRANTY_YEARS.toString(),
                isYears = true
            )
        }
        validateAndRecalculate()
    }

    /** Atajo de los chips de duración: fija la cantidad en años de una vez. */
    fun onQuickDurationSelected(years: Int) {
        updateState { it.copy(warrantyMonths = years.toString(), isYears = true) }
        validateAndRecalculate()
    }

    private fun checkCanAdd() {
        viewModelScope.launch {
            val canAdd = checkPremiumLimitUseCase()
            updateState { it.copy(canAdd = canAdd) }
        }
    }

    fun onProductNameChanged(name: String) {
        // Trunca silenciosamente si excede el máximo (mejor UX que ignorar nuevos
        // caracteres). El TextField muestra un contador para que el usuario lo vea.
        val capped = if (name.length > MAX_PRODUCT_NAME_LENGTH) {
            name.take(MAX_PRODUCT_NAME_LENGTH)
        } else name
        updateState { it.copy(productName = capped, productNameError = null) }
    }

    fun onCategoryChanged(category: WarrantyCategory) {
        updateState { it.copy(category = category) }
    }

    fun onStoreChanged(store: String) {
        updateState { it.copy(store = store.take(MAX_STORE_LENGTH)) }
    }

    /**
     * Solo cifras y separadores, con tope de longitud. Pegar "1e308" o una cadena enorme
     * llegaba tal cual al parser y al estado guardado.
     */
    fun onPriceChanged(price: String) {
        val cleaned = price.filter { it.isDigit() || it == ',' || it == '.' }.take(MAX_PRICE_LENGTH)
        updateState { it.copy(price = cleaned) }
    }

    fun onPurchaseDateChanged(date: LocalDate) {
        updateState { it.copy(purchaseDate = date, purchaseDateError = null) }
        validateAndRecalculate()
    }

    fun onWarrantyMonthsChanged(months: String) {
        // Limita la longitud antes de procesar para evitar overflow de Long.
        val trimmed = months.take(MAX_INPUT_DIGITS).filter { it.isDigit() }
        updateState { it.copy(warrantyMonths = trimmed) }
        validateAndRecalculate()
    }

    fun onIsYearsChanged(isYears: Boolean) {
        updateState { it.copy(isYears = isYears) }
        validateAndRecalculate()
    }

    fun onPhotoChanged(path: String?) {
        updateState { it.copy(receiptPhotoPath = path) }
    }

    /**
     * Copia la foto elegida fuera del hilo principal.
     *
     * El callback del photo picker corre en el hilo de UI, y con un URI de Google Photos
     * que solo existe en la nube la copia se descarga por red en ese momento: la pantalla
     * se congelaba varios segundos, con riesgo de ANR. Va en `viewModelScope` para que
     * girar la pantalla a media copia no la cancele.
     */
    fun onPhotoPicked(uri: Uri, copyToInternal: (Uri) -> String?) {
        if (_state.value.isPhotoLoading) return
        updateState { it.copy(isPhotoLoading = true) }
        viewModelScope.launch {
            val path = withContext(ioDispatcher) { copyToInternal(uri) }
            updateState {
                // Una copia fallida deja la foto anterior, no la borra.
                it.copy(isPhotoLoading = false, receiptPhotoPath = path ?: it.receiptPhotoPath)
            }
        }
    }

    /**
     * Sin tope, unas notas muy largas hacían crecer el estado guardado en cada pulsación
     * (el sistema lo limita a ~1 MB por proceso) y podían no caber al leer la fila.
     */
    fun onNotesChanged(notes: String) {
        updateState { it.copy(notes = notes.take(MAX_NOTES_LENGTH)) }
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

        updateState { it.copy(warrantyMonthsError = error, warrantyEndDate = newEndDate) }
    }

    fun onSave() {
        val current = _state.value
        // Doble guarda: isSaving bloquea double-tap durante la inserción, isSaved
        // bloquea re-inserciones tras un guardado exitoso (por si la navegación
        // fuera aún no ha ocurrido).
        if (current.isSaving || current.isSaved) return
        // Con la copia de la foto en marcha, guardar persistía la garantía sin el ticket
        // (o con el anterior) y el fichero recién copiado quedaba huérfano en disco.
        if (current.isPhotoLoading) return

        // Guardar está siempre activo: al pulsarlo se marcan a la vez todos los campos que
        // faltan. Antes el botón se apagaba sin explicar qué faltaba.
        val nameError = AddEditError.NameRequired.takeIf { current.productName.isBlank() }
        val dateError = AddEditError.DateRequired.takeIf { current.purchaseDate == null }
        val durationError = current.warrantyMonthsError
            ?: AddEditError.DurationRequired.takeIf {
                current.purchaseDate != null && current.warrantyEndDate == null
            }
        if (nameError != null || dateError != null || durationError != null) {
            updateState {
                it.copy(
                    productNameError = nameError,
                    purchaseDateError = dateError,
                    warrantyMonthsError = durationError
                )
            }
            return
        }
        if (current.purchaseDate == null || current.warrantyEndDate == null) return
        if (!current.isEditMode && !current.canAdd) {
            updateState { it.copy(formError = AddEditError.PremiumLimit) }
            return
        }

        updateState { it.copy(isSaving = true) }

        viewModelScope.launch {
            val warranty = Warranty(
                // Solo se conserva el id si de verdad se cargó esa garantía para editarla.
                id = if (current.isEditMode) warrantyId ?: 0 else 0,
                productName = current.productName.trim(),
                category = current.category,
                store = current.store.trim(),
                price = current.price.toPriceOrNull(),
                purchaseDate = current.purchaseDate,
                warrantyEndDate = current.warrantyEndDate,
                receiptPhotoPath = current.receiptPhotoPath,
                notes = current.notes.trim(),
                createdAt = current.createdAt ?: LocalDateTime.now()
            )

            val result = if (current.isEditMode) {
                updateWarrantyUseCase(warranty)
            } else {
                addWarrantyUseCase(warranty)
            }

            when (result) {
                is Result.Success -> updateState { it.copy(isSaving = false, isSaved = true) }
                // El tope del plan gratuito se revalida al insertar, así que puede llegar
                // aquí aunque la pantalla se abriera con hueco disponible. Se muestra
                // como el error localizable de siempre, no como texto crudo.
                is Result.Error -> if (result.exception is PremiumLimitReachedException) {
                    updateState { it.copy(isSaving = false, formError = AddEditError.PremiumLimit) }
                } else {
                    updateState { it.copy(isSaving = false, error = result.exception.message) }
                }
            }
        }
    }

    /**
     * Los consume la pantalla tras mostrarlos. Sin esto el error se quedaría en el estado
     * y el snackbar no volvería a salir si el usuario repite la misma acción fallida.
     */
    fun onErrorShown() {
        updateState { it.copy(error = null) }
    }

    fun onFormErrorShown() {
        updateState { it.copy(formError = null) }
    }

    /**
     * Solo se respaldan los campos que el usuario ha tecleado. `isSaving` / `isSaved` son
     * transitorios: restaurarlos dejaría el formulario bloqueado o navegaría solo.
     */
    private fun saveDraft(state: AddEditState) {
        val handle = savedStateHandle ?: return
        handle[KEY_HAS_DRAFT] = true
        handle[KEY_EDIT_MODE] = state.isEditMode
        handle[KEY_PRODUCT_NAME] = state.productName
        handle[KEY_CATEGORY] = state.category.name
        handle[KEY_STORE] = state.store
        handle[KEY_PRICE] = state.price
        handle[KEY_PURCHASE_DATE] = state.purchaseDate?.toEpochDay() ?: NO_DATE
        handle[KEY_MONTHS] = state.warrantyMonths
        handle[KEY_IS_YEARS] = state.isYears
        handle[KEY_PHOTO] = state.receiptPhotoPath
        handle[KEY_NOTES] = state.notes
        handle[KEY_CREATED_AT] = state.createdAt?.toString()
    }

    private fun restoreDraft(): AddEditState? {
        val handle = savedStateHandle ?: return null
        if (handle.get<Boolean>(KEY_HAS_DRAFT) != true) return null

        val purchaseEpochDay = handle.get<Long>(KEY_PURCHASE_DATE) ?: NO_DATE
        return AddEditState(
            isEditMode = handle.get<Boolean>(KEY_EDIT_MODE) ?: false,
            productName = handle.get<String>(KEY_PRODUCT_NAME).orEmpty().take(MAX_PRODUCT_NAME_LENGTH),
            category = handle.get<String>(KEY_CATEGORY)
                ?.let { name -> WarrantyCategory.entries.firstOrNull { it.name == name } }
                ?: WarrantyCategory.ELECTRONICS,
            store = handle.get<String>(KEY_STORE).orEmpty().take(MAX_STORE_LENGTH),
            price = handle.get<String>(KEY_PRICE).orEmpty().take(MAX_PRICE_LENGTH),
            purchaseDate = purchaseEpochDay.takeIf { it != NO_DATE }?.let(LocalDate::ofEpochDay),
            warrantyMonths = handle.get<String>(KEY_MONTHS).orEmpty(),
            isYears = handle.get<Boolean>(KEY_IS_YEARS) ?: false,
            receiptPhotoPath = handle.get<String>(KEY_PHOTO),
            notes = handle.get<String>(KEY_NOTES).orEmpty().take(MAX_NOTES_LENGTH),
            createdAt = handle.get<String>(KEY_CREATED_AT)
                ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        )
    }

    companion object {
        private const val MAX_WARRANTY_MONTHS = 600L // 50 años
        /** Garantía legal en España para compras desde 2022 (RDL 7/2021). */
        const val DEFAULT_WARRANTY_YEARS = 3
        val QUICK_DURATION_YEARS = listOf(1, 2, 3, 5)
        private const val MAX_INPUT_DIGITS = 4 // 9999 cabe sobrado en Long
        const val MAX_PRODUCT_NAME_LENGTH = 60
        const val MAX_STORE_LENGTH = 80
        const val MAX_NOTES_LENGTH = 2000
        private const val MAX_PRICE_LENGTH = 12

        /** Centinela: SavedStateHandle no distingue "sin fecha" de un epochDay legítimo. */
        private const val NO_DATE = Long.MIN_VALUE

        private const val KEY_HAS_DRAFT = "draft_present"
        private const val KEY_EDIT_MODE = "draft_edit_mode"
        private const val KEY_PRODUCT_NAME = "draft_product_name"
        private const val KEY_CATEGORY = "draft_category"
        private const val KEY_STORE = "draft_store"
        private const val KEY_PRICE = "draft_price"
        private const val KEY_PURCHASE_DATE = "draft_purchase_date"
        private const val KEY_MONTHS = "draft_months"
        private const val KEY_IS_YEARS = "draft_is_years"
        private const val KEY_PHOTO = "draft_photo"
        private const val KEY_NOTES = "draft_notes"
        private const val KEY_CREATED_AT = "draft_created_at"
    }
}
