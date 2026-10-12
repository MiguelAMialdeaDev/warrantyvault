package com.miguelcode.warrantyvault.database.repository

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * El `catch` de [PreferencesRepositoryImpl.safeData] solo cubre las lecturas: un `edit`
 * sobre un fichero corrupto seguía lanzando CorruptionException (al arrancar, desde la
 * sincronización de compras), y la app volvía al crash loop. El handler sustituye el
 * fichero por uno vacío antes de que la excepción llegue a nadie.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "warranty_vault_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    /**
     * `dataStore.data` lanza IOException si el fichero no se puede leer y
     * CorruptionException si no deserializa, y el contrato de DataStore es que lo capture
     * el consumidor. Sin este `catch`, un fichero de preferencias corrupto propagaba la
     * excepción a todos los colectores: la app quedaba en un crash loop nada más arrancar
     * y solo se salía borrando los datos. Volviendo a valores por defecto, como mucho se
     * pierden las preferencias.
     */
    private val safeData: Flow<Preferences>
        get() = context.dataStore.data.catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    override val isPremiumFlow: Flow<Boolean>
        get() = safeData.map { it[KEY_IS_PREMIUM] ?: false }

    override val isOnboardingDoneFlow: Flow<Boolean>
        get() = safeData.map { it[KEY_ONBOARDING_DONE] ?: false }

    override val themeModeFlow: Flow<String>
        get() = safeData.map { it[KEY_THEME_MODE] ?: "system" }

    override val notificationsEnabledFlow: Flow<Boolean>
        get() = safeData.map { it[KEY_NOTIFICATIONS_ENABLED] ?: notificationsDefault }

    /**
     * Por debajo de API 33 no existe el permiso runtime de notificaciones: el sistema las
     * concede de serie y no hay diálogo cuyo resultado ponga la preferencia a true. Con un
     * default en false, las alertas de vencimiento (la función central de la app) quedaban
     * apagadas para siempre en todo Android 8.0-12 salvo que el usuario descubriese el
     * interruptor de Ajustes.
     *
     * En API 33+ manda el permiso, pero leído en vivo y no como un false fijo: quien
     * instaló en Android 12 y nunca tocó el interruptor no tiene nada guardado, y al
     * actualizar a 13 el sistema le concede el permiso de oficio. Con el false fijo, el
     * siguiente arranque cancelaba todas sus alertas sin avisar.
     *
     * Solo es el valor por defecto; en cuanto el usuario toca el interruptor, gana lo
     * guardado.
     */
    private val notificationsDefault: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.getSystemService(NotificationManager::class.java)
                ?.areNotificationsEnabled() == true

    override suspend fun isPremium(): Boolean =
        safeData.first()[KEY_IS_PREMIUM] ?: false

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
