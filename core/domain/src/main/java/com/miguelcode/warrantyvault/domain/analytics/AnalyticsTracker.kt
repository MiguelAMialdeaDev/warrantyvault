package com.miguelcode.warrantyvault.domain.analytics

/**
 * Tracker de eventos para Firebase Analytics y de errores para Crashlytics.
 *
 * El proyecto usa una implementación NoOp por defecto, intercambiable por
 * `FirebaseAnalyticsTracker` cuando exista `app/google-services.json`.
 *
 * Eventos reservados (Firebase los reconoce):
 *  - `screen_view` con params `screen_name`, `screen_class`
 *  - `select_content` con params `content_type`, `item_id`
 *
 * Eventos custom de WarrantyVault:
 *  - `warranty_added`        → al guardar una nueva warranty (success)
 *  - `warranty_edited`       → al actualizar una warranty
 *  - `warranty_deleted`      → al eliminar
 *  - `premium_clicked`       → al pulsar CTA premium (banner, FAB, settings)
 *  - `premium_purchased`     → al completar compra (acknowledged)
 *  - `notification_received` → al disparar el worker
 *  - `onboarding_completed`  → al terminar las 3 páginas
 *  - `search_performed`      → con `query_length` (no la query, por privacidad)
 */
interface AnalyticsTracker {
    /** Registra un evento custom. params solo debe contener tipos primitivos. */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    /** Registra un crash no fatal con contexto opcional. */
    fun logNonFatal(throwable: Throwable, message: String? = null)

    /** Asocia el plan del usuario (free/premium) a la sesión. */
    fun setUserProperty(key: String, value: String?)
}

/** Constantes de nombres de evento — uso desde callsites. */
object AnalyticsEvents {
    const val WARRANTY_ADDED = "warranty_added"
    const val WARRANTY_EDITED = "warranty_edited"
    const val WARRANTY_DELETED = "warranty_deleted"
    const val PREMIUM_CLICKED = "premium_clicked"
    const val PREMIUM_PURCHASED = "premium_purchased"
    const val NOTIFICATION_RECEIVED = "notification_received"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val SEARCH_PERFORMED = "search_performed"
}

object UserProperties {
    const val PLAN = "plan" // values: "free" | "premium"
}
