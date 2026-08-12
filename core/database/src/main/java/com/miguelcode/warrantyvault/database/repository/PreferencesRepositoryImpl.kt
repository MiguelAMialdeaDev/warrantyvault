package com.miguelcode.warrantyvault.database.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "warranty_vault_prefs")

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    override val isPremiumFlow: Flow<Boolean>
        get() = context.dataStore.data.map { it[KEY_IS_PREMIUM] ?: false }

    override val isOnboardingDoneFlow: Flow<Boolean>
        get() = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    override val themeModeFlow: Flow<String>
        get() = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }

    override val notificationsEnabledFlow: Flow<Boolean>
        get() = context.dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: false }

    override suspend fun isPremium(): Boolean =
        context.dataStore.data.first()[KEY_IS_PREMIUM] ?: false

    override suspend fun setIsPremium(value: Boolean) {
        context.dataStore.edit { it[KEY_IS_PREMIUM] = value }
    }

    override suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    companion object {
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }
}
