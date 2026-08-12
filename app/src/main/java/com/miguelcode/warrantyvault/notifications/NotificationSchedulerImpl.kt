package com.miguelcode.warrantyvault.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Data
import com.miguelcode.warrantyvault.R
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NotificationSchedulerImpl(private val context: Context) : NotificationScheduler {

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

            val delayMillis = alertDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - System.currentTimeMillis()

            if (delayMillis <= 0) return@forEach

            val inputData = Data.Builder()
                .putLong(NotificationConstants.INPUT_WARRANTY_ID, warranty.id)
                .putString(NotificationConstants.INPUT_PRODUCT_NAME, warranty.productName)
                .putLong(NotificationConstants.INPUT_DAYS_LEFT, daysBefore)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WarrantyExpirationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(tagFor(warranty.id))
                .addTag(NotificationConstants.WORK_TAG_ALL)
                .build()

            workManager.enqueue(workRequest)
        }
    }

    override fun cancelAlerts(warrantyId: Long) {
        workManager.cancelAllWorkByTag(tagFor(warrantyId))
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
