package com.miguelcode.warrantyvault.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Data
import com.miguelcode.warrantyvault.R
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NotificationSchedulerImpl(
    private val context: Context,
    private val deliveredAlerts: DeliveredAlertsStore
) : NotificationScheduler {

    private val workManager = WorkManager.getInstance(context)

    init {
        createChannel()
    }

    override fun scheduleExpirationAlerts(warranty: Warranty) {
        val today = LocalDate.now()
        val endDate = warranty.warrantyEndDate

        NotificationConstants.ALERT_DAYS_BEFORE.forEach { daysBefore ->
            val alertDate = endDate.minusDays(daysBefore)
            if (alertDate.isBefore(today)) return@forEach
            // Ya se mostró: reprogramarlo con delay 0 lo haría reaparecer cada vez que el
            // usuario abre la app ese día, después de haberlo descartado.
            if (deliveredAlerts.isDelivered(warranty.id, daysBefore)) return@forEach

            // El aviso de HOY cuya hora ya pasó se encola con delay 0 en lugar de
            // descartarse. Antes se perdía para siempre: el sync de cada arranque lo
            // reprogramaba y lo saltaba por tener el instante en el pasado.
            val delayMillis = (alertDate
                .atTime(NotificationConstants.ALERT_HOUR, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis())
                .coerceAtLeast(0L)

            val inputData = Data.Builder()
                .putLong(NotificationConstants.INPUT_WARRANTY_ID, warranty.id)
                .putString(NotificationConstants.INPUT_PRODUCT_NAME, warranty.productName)
                .putLong(NotificationConstants.INPUT_DAYS_LEFT, daysBefore)
                .putLong(NotificationConstants.INPUT_END_DATE_EPOCH_DAY, endDate.toEpochDay())
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WarrantyExpirationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(tagFor(warranty.id))
                .addTag(NotificationConstants.WORK_TAG_ALL)
                .build()

            // Trabajo único por (garantía, aviso) con KEEP: reprogramar en cada arranque
            // ya no duplica ni destruye un aviso pendiente. Esa idempotencia es la que
            // antes daba el cancelAll() del sync, que a cambio perdía los avisos del día.
            workManager.enqueueUniqueWork(
                uniqueNameFor(warranty.id, daysBefore),
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override fun cancelAlerts(warrantyId: Long, forgetDelivered: Boolean) {
        workManager.cancelAllWorkByTag(tagFor(warrantyId))
        // Al borrar la garantía o cambiar su fecha de fin, el registro de avisos entregados
        // ya no sirve y se borra. Si la fecha no cambió se conserva: borrarlo hacía
        // reaparecer el aviso de hoy al corregir, por ejemplo, las notas.
        if (forgetDelivered) deliveredAlerts.clearFor(warrantyId)
    }

    /**
     * Cancela las alertas de todas las garantías de una vez, sin tocar ningún otro
     * trabajo encolado por la app o por sus librerías: solo se borra lo que lleva
     * nuestro tag común.
     */
    override fun cancelAll() {
        workManager.cancelAllWorkByTag(NotificationConstants.WORK_TAG_ALL)
    }

    private fun tagFor(warrantyId: Long) = "${NotificationConstants.WORK_TAG_PREFIX}$warrantyId"

    private fun uniqueNameFor(warrantyId: Long, daysBefore: Long) =
        "${NotificationConstants.WORK_TAG_PREFIX}${warrantyId}_$daysBefore"

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_description)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
