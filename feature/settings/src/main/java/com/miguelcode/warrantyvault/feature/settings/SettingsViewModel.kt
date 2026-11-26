package com.miguelcode.warrantyvault.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val syncExpirationAlerts: SyncExpirationAlertsUseCase,
    private val billingManager: BillingManagerContract
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = preferencesRepository.isPremiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = preferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _restoring = MutableStateFlow(false)
    val restoring: StateFlow<Boolean> = _restoring.asStateFlow()

    private val _restoreResults = Channel<RestoreResult>(Channel.BUFFERED)
    /** Resultado de "Restaurar compras", que la pantalla enseña una sola vez. */
    val restoreResults: Flow<RestoreResult> = _restoreResults.receiveAsFlow()

    private var notificationsJob: Job? = null

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    /**
     * Guarda la preferencia y, acto seguido, la aplica de verdad: al desactivar borra
     * las alertas encoladas y al reactivar las vuelve a programar. Antes esto solo
     * escribía el booleano, así que el interruptor no apagaba nada y el usuario seguía
     * recibiendo avisos que creía haber desactivado.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        // Cancelar el anterior evita que dos pulsaciones rápidas dejen el estado final
        // decidido por cuál de las dos corrutinas termine antes, en vez de por la última
        // pulsación del usuario.
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            syncExpirationAlerts()
        }
    }

    /**
     * Pide a Play las suscripciones de la cuenta y espera un rato a que el flag de
     * premium suba. Play restaura solo al arrancar, pero quien cambia de móvil o de
     * cuenta busca este botón y, sin él, no tenía forma de forzarlo ni de saber si
     * su compra se había encontrado.
     */
    fun restorePurchases() {
        if (_restoring.value) return
        _restoring.value = true
        viewModelScope.launch {
            billingManager.refreshPurchases()
            val found = withTimeoutOrNull(RESTORE_TIMEOUT_MS) {
                preferencesRepository.isPremiumFlow.firstOrNull { it }
            } == true
            _restoring.value = false
            _restoreResults.send(if (found) RestoreResult.Restored else RestoreResult.NothingFound)
        }
    }

    enum class RestoreResult { Restored, NothingFound }

    internal companion object {
        const val RESTORE_TIMEOUT_MS = 10_000L
    }
}
