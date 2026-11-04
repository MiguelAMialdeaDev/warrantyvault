package com.miguelcode.warrantyvault.analytics

import android.content.Context
import android.os.Bundle
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker

/**
 * Implementación real Firebase. Activar cuando esté `app/google-services.json`.
 *
 * ANTES de activarla, sin excepción:
 *   - Actualizar la política de privacidad (es y en, aquí y en el repo de GitHub Pages):
 *     hoy promete que la app no recoge datos, y Analytics y Crashlytics sí los recogen.
 *   - Actualizar la sección de Seguridad de los datos de Play Console.
 *   - Pedir consentimiento antes de enviar nada (opt-in) y no mandar nunca datos de las
 *     garantías (nombre, tienda, notas): solo eventos y números.
 *   - Volver a declarar los plugins de Firebase en el build.gradle.kts raíz (se quitaron
 *     por no usarse) y restringir la clave de Firebase al paquete y a las huellas SHA-1.
 *
 * Pasos técnicos:
 *
 *   1. Añadir a `app/build.gradle.kts`:
 *      ```
 *      plugins {
 *          // ...
 *          if (file("google-services.json").exists()) {
 *              id("com.google.gms.google-services")
 *              id("com.google.firebase.crashlytics")
 *          }
 *      }
 *
 *      dependencies {
 *          implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
 *          implementation("com.google.firebase:firebase-analytics-ktx")
 *          implementation("com.google.firebase:firebase-crashlytics-ktx")
 *      }
 *      ```
 *
 *   2. En `AppModule.kt`, cambiar el binding:
 *      ```
 *      single<AnalyticsTracker> { FirebaseAnalyticsTracker(androidContext()) }
 *      ```
 *
 *   3. Descomentar el cuerpo de los métodos abajo.
 *
 * Mientras tanto se queda como stub que llama al NoOp para no romper compilación
 * sin las dependencias de Firebase.
 */
class FirebaseAnalyticsTracker(private val context: Context) : AnalyticsTracker {

    // private val analytics = FirebaseAnalytics.getInstance(context)
    // private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        // val bundle = Bundle().apply {
        //     params.forEach { (k, v) ->
        //         when (v) {
        //             is String -> putString(k, v)
        //             is Long -> putLong(k, v)
        //             is Int -> putInt(k, v)
        //             is Double -> putDouble(k, v)
        //             is Boolean -> putBoolean(k, v.toString().toBoolean())
        //             null -> putString(k, "")
        //             else -> putString(k, v.toString())
        //         }
        //     }
        // }
        // analytics.logEvent(name, bundle)
    }

    override fun logNonFatal(throwable: Throwable, message: String?) {
        // if (message != null) crashlytics.log(message)
        // crashlytics.recordException(throwable)
    }

    override fun setUserProperty(key: String, value: String?) {
        // analytics.setUserProperty(key, value)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun unused(b: Bundle) = Unit // evita warning hasta activar
}
