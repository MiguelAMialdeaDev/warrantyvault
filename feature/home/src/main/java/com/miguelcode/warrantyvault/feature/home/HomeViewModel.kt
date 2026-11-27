package com.miguelcode.warrantyvault.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getWarrantiesUseCase: GetWarrantiesUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadWarranties()
    }

    private var loadJob: Job? = null

    private fun loadWarranties() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getWarrantiesUseCase(),
                preferencesRepository.isPremiumFlow
            ) { warranties, isPremium ->
                warranties to isPremium
            }
                // Un fallo leyendo Room cerraba la app. Tampoco vale enseñar la lista vacía:
                // el usuario creería que ha perdido sus garantías.
                .catch { _state.update { it.copy(isLoading = false, loadError = true) } }
                .collect { (warranties, isPremium) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadError = false,
                        warranties = warranties,
                        isPremium = isPremium,
                        // Las archivadas no ocupan hueco del plan gratuito.
                        usedSlots = warranties.count { !it.isArchived }
                    )
                }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(isLoading = true, loadError = false) }
        loadWarranties()
    }

    fun onFilterSelected(filter: WarrantyFilter) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
