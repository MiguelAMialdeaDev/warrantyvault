package com.miguelcode.warrantyvault.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.DeleteWarrantyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val warrantyId: Long,
    private val warrantyRepository: WarrantyRepository,
    private val deleteWarrantyUseCase: DeleteWarrantyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        observeWarranty()
    }

    /**
     * Suscribe al Flow del repo para que la pantalla se actualice automáticamente
     * cuando se edita la warranty desde otra pantalla (Edit → Save vuelve aquí
     * y el state refleja los cambios sin re-fetch manual).
     */
    private fun observeWarranty() {
        viewModelScope.launch {
            warrantyRepository.getWarrantyByIdFlow(warrantyId).collect { warranty ->
                if (warranty == null) {
                    // Si nunca llegamos a cargarla, el id no existe (garantía borrada
                    // antes de restaurar el estado, enlace viejo): sin marcarlo, ninguna
                    // rama de la pantalla se cumplía y quedaba completamente en blanco.
                    // Si ya la teníamos, fue un borrado desde otra pantalla y se conserva
                    // lo mostrado para no disparar la navegación si borramos nosotros.
                    _state.update { it.copy(isLoading = false, notFound = it.warranty == null) }
                } else {
                    _state.update { it.copy(isLoading = false, warranty = warranty) }
                }
            }
        }
    }

    fun onDelete() {
        val warranty = _state.value.warranty ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            when (val result = deleteWarrantyUseCase(warranty)) {
                is Result.Success -> _state.update { it.copy(isDeleting = false, isDeleted = true) }
                is Result.Error -> _state.update {
                    it.copy(isDeleting = false, error = result.exception.message)
                }
            }
        }
    }
}
