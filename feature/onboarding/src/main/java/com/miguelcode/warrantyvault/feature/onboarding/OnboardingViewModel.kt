package com.miguelcode.warrantyvault.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private var completing = false

    /**
     * Entre el tap y el final de la escritura en DataStore pasan decenas de ms. Sin esta
     * guarda, dos taps rápidos (o "Saltar" seguido de "Empezar") encolaban dos corrutinas:
     * la primera navegación saca Onboarding del back stack y la segunda apila un segundo
     * Home encima, porque su `popUpTo` ya no encuentra la ruta que debía limpiar.
     */
    fun completeOnboarding(onDone: () -> Unit) {
        if (completing) return
        completing = true
        viewModelScope.launch {
            preferencesRepository.setOnboardingDone()
            analytics.logEvent(AnalyticsEvents.ONBOARDING_COMPLETED)
            onDone()
        }
    }
}
