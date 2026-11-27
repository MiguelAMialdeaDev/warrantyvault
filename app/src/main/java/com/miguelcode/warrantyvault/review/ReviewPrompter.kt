package com.miguelcode.warrantyvault.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Pide valoración con la hoja de Google Play, sin salir de la app. Se pide en un momento
 * de éxito (acaba de guardar su segunda garantía) y como mucho una vez cada 90 días:
 * Play ya limita cuántas veces la enseña, pero pedirla a destiempo gasta el cupo.
 */
class ReviewPrompter(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val manager = ReviewManagerFactory.create(context)

    fun maybeAsk(activity: Activity, now: Long = System.currentTimeMillis()) {
        if (!shouldAsk(prefs.getLong(KEY_LAST_ASKED, 0L), now)) return
        prefs.edit().putLong(KEY_LAST_ASKED, now).apply()
        manager.requestReviewFlow().addOnSuccessListener { info ->
            if (!activity.isFinishing) manager.launchReviewFlow(activity, info)
        }
    }

    companion object {
        private const val PREFS = "review_prompt"
        private const val KEY_LAST_ASKED = "last_asked_ms"
        private const val MIN_INTERVAL_MS = 90L * 24 * 60 * 60 * 1000

        /** Nunca se ha pedido, o hace más de 90 días. */
        fun shouldAsk(lastAskedMs: Long, now: Long): Boolean =
            lastAskedMs == 0L || now - lastAskedMs >= MIN_INTERVAL_MS

        /** El momento: el recuento sube y llega a 2 o más (acaba de guardar una). */
        fun isSuccessMoment(previousCount: Int?, count: Int): Boolean =
            previousCount != null && count > previousCount && count >= 2
    }
}
