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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarrantyExpirationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val analytics: AnalyticsTracker by inject()

    override suspend fun doWork(): Result {
        val warrantyId = inputData.getLong(NotificationConstants.INPUT_WARRANTY_ID, -1)
        val productName = inputData.getString(NotificationConstants.INPUT_PRODUCT_NAME) ?: return Result.failure()
        val daysLeft = inputData.getLong(NotificationConstants.INPUT_DAYS_LEFT, 0)

        if (warrantyId < 0) return Result.failure()

        showNotification(warrantyId, productName, daysLeft)
        analytics.logEvent(
            AnalyticsEvents.NOTIFICATION_RECEIVED,
            mapOf("days_left" to daysLeft)
        )
        return Result.success()
    }

    private fun showNotification(warrantyId: Long, productName: String, daysLeft: Long) {
        val context = applicationContext

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
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
            1L -> context.getString(R.string.notif_title_last_day)
            in 2L..7L -> context.getString(R.string.notif_title_days_left, daysLeft)
            else -> context.getString(R.string.notif_title_expiring)
        }

        val message = if (daysLeft == 1L) {
            context.getString(R.string.notif_message_last_day, productName)
        } else {
            context.getString(R.string.notif_message_days_left, productName, daysLeft)
        }

        val notification = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            (warrantyId.toString() + daysLeft.toString()).hashCode(),
            notification
        )
    }
}
