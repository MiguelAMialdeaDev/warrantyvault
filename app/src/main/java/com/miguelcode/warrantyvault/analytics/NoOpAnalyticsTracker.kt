package com.miguelcode.warrantyvault.analytics

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker

/**
 * Implementación que no hace nada. Activa cuando aún no hay `google-services.json`.
 * Permite al código llamar `tracker.logEvent(...)` sin condicionales — ya está la
 * arquitectura lista para v1.0.1 cuando se conecte Firebase.
 */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    override fun logNonFatal(throwable: Throwable, message: String?) = Unit
    override fun setUserProperty(key: String, value: String?) = Unit
}
