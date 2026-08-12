package com.miguelcode.warrantyvault.notifications

object NotificationConstants {
    /** ID estable del canal — NO traducir, lo usa Android para identificar el canal. */
    const val CHANNEL_ID = "warranty_expiration"

    const val WORK_TAG_PREFIX = "warranty_alert_"

    /**
     * Tag que llevan TODAS las alertas, además del tag propio de cada garantía.
     * `cancelAllWorkByTag` compara el tag completo, no por prefijo, así que sin un tag
     * común no hay forma de cancelarlas todas de golpe. Esa era la razón de que
     * `cancelAll()` estuviese vacío.
     */
    const val WORK_TAG_ALL = "warranty_alert_all"

    // Días antes del vencimiento en los que se dispara la alerta
    val ALERT_DAYS_BEFORE = listOf(30L, 14L, 7L, 1L)

    // Input data keys
    const val INPUT_WARRANTY_ID = "warranty_id"
    const val INPUT_PRODUCT_NAME = "product_name"
    const val INPUT_DAYS_LEFT = "days_left"
}
