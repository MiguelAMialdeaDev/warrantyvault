package com.miguelcode.warrantyvault.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    private fun loadWarranties() {
        viewModelScope.launch {
            combine(
                getWarrantiesUseCase(),
                preferencesRepository.isPremiumFlow
            ) { warranties, isPremium ->
                warranties to isPremium
            }.collect { (warranties, isPremium) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        warranties = warranties,
                        isPremium = isPremium,
                        usedSlots = warranties.size
                    )
                }
            }
        }
    }

    fun onFilterSelected(filter: WarrantyFilter) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
