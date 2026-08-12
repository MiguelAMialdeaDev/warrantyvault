package com.miguelcode.warrantyvault.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.miguelcode.warrantyvault.MainActivity
import com.miguelcode.warrantyvault.R
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WarrantyExpirationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val analytics: AnalyticsTracker by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val deliveredAlerts: DeliveredAlertsStore by inject()

    override suspend fun doWork(): Result {
        val warrantyId = inputData.getLong(NotificationConstants.INPUT_WARRANTY_ID, -1)
        val productName = inputData.getString(NotificationConstants.INPUT_PRODUCT_NAME) ?: return Result.failure()

        if (warrantyId < 0) return Result.failure()

        // Última comprobación antes de molestar al usuario: un worker encolado sobrevive
        // a que apague el interruptor, y cancelar trabajo en WorkManager no es inmediato.
        if (!preferencesRepository.notificationsEnabledFlow.first()) return Result.success()

        // Doze puede diferir la ejecución horas o días, así que los días restantes se
        // recalculan contra el reloj real. El valor congelado al programar llegaba
        // desfasado ("te quedan 7 días" dos días tarde).
        val endDateEpochDay = inputData.getLong(
            NotificationConstants.INPUT_END_DATE_EPOCH_DAY,
            NO_END_DATE
        )
        val daysLeft = if (endDateEpochDay != NO_END_DATE) {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(endDateEpochDay))
        } else {
            // Trabajo encolado por una versión anterior que aún no llevaba la fecha.
            inputData.getLong(NotificationConstants.INPUT_DAYS_LEFT, 0)
        }

        // Ya vencida cuando por fin se ejecuta: el aviso previo al vencimiento no aplica.
        if (daysLeft < 0) return Result.success()

        val scheduledDaysBefore = inputData.getLong(NotificationConstants.INPUT_DAYS_LEFT, 0)
        val shown = showNotification(warrantyId, productName, daysLeft)
        // Se marca con la antelación con la que se programó, no con los días reales: es la
        // que identifica el aviso y la que consulta el scheduler para no repetirlo.
        if (shown) deliveredAlerts.markDelivered(warrantyId, scheduledDaysBefore)
        // Solo cuenta como entregada si de verdad se publicó: con el permiso denegado la
        // métrica reportaba avisos que el usuario nunca llegó a ver.
        if (shown) {
            analytics.logEvent(
                AnalyticsEvents.NOTIFICATION_RECEIVED,
                mapOf("days_left" to daysLeft)
            )
        }
        return Result.success()
    }

    private fun showNotification(warrantyId: Long, productName: String, daysLeft: Long): Boolean {
        val context = applicationContext

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            warrantyId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (daysLeft) {
            0L -> context.getString(R.string.notif_title_expires_today)
            1L -> context.getString(R.string.notif_title_last_day)
            in 2L..7L -> context.getString(R.string.notif_title_days_left, daysLeft)
            else -> context.getString(R.string.notif_title_expiring)
        }

        val message = when (daysLeft) {
            0L -> context.getString(R.string.notif_message_expires_today, productName)
            1L -> context.getString(R.string.notif_message_last_day, productName)
            else -> context.getString(R.string.notif_message_days_left, productName, daysLeft)
        }

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // El aviso del día en curso puede reencolarse al abrir la app; con el mismo
            // id, esto actualiza la notificación en silencio en vez de volver a sonar.
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            (warrantyId.toString() + daysLeft.toString()).hashCode(),
            notification
        )
        return true
    }

    private companion object {
        /** Centinela: `Data.getLong` no distingue "ausente" de un valor legítimo. */
        const val NO_END_DATE = Long.MIN_VALUE
    }
}
