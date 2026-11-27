package com.miguelcode.warrantyvault.crash

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Informes de fallos con Crashlytics, sin Analytics. La recogida arranca apagada en el
 * manifiesto y solo se enciende aquí según la preferencia del usuario, así que nada sale
 * del móvil si la ha desactivado en Ajustes.
 *
 * Sin google-services.json (build sin proyecto de Firebase) FirebaseApp no existe y esto
 * no hace nada: la app funciona igual.
 */
object CrashReporting {

    fun isAvailable(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun setEnabled(context: Context, enabled: Boolean) {
        if (!isAvailable(context)) return
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }
}
