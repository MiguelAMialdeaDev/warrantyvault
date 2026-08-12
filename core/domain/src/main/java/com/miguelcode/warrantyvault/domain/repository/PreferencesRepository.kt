package com.miguelcode.warrantyvault.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val isPremiumFlow: Flow<Boolean>
    val isOnboardingDoneFlow: Flow<Boolean>
    val themeModeFlow: Flow<String> // "system", "light", "dark"
    val notificationsEnabledFlow: Flow<Boolean>
    suspend fun isPremium(): Boolean
    suspend fun setIsPremium(value: Boolean)
    suspend fun setOnboardingDone()
    suspend fun setThemeMode(mode: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
