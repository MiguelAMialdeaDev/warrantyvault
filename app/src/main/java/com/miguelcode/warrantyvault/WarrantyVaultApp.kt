package com.miguelcode.warrantyvault

import android.app.Application
import com.miguelcode.warrantyvault.di.appModules
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.storage.OrphanPhotoSweeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class WarrantyVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@WarrantyVaultApp)
            modules(appModules)
        }
        // Fuerza la creación del scheduler → crea el canal de notificaciones
        get<NotificationScheduler>()
        // Billing conecta al arrancar, no solo al abrir el paywall. Es lo que revoca el
        // premium de quien ha cancelado, lo restaura tras reinstalar o cambiar de móvil,
        // y reintenta el acknowledge de compras que quedaran sin confirmar.
        get<BillingManagerContract>().connect()

        // Las alertas viven en WorkManager, que no viaja en la copia de seguridad: al
        // cambiar de móvil o reinstalar, el usuario recupera sus garantías pero se queda
        // sin un solo aviso programado. Esto las reconstruye en cada arranque. Va fuera
        // del hilo principal porque lee base de datos y DataStore, y es idempotente, así
        // que repetirlo en cada arranque no duplica nada.
        val syncAlerts = get<SyncExpirationAlertsUseCase>()
        val warrantyRepository = get<WarrantyRepository>()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            syncAlerts()
            // Fotos copiadas al elegirlas pero nunca guardadas (salir sin guardar, cambiar
            // de foto): sin este barrido ocupaban espacio para siempre.
            runCatching {
                val referenced = warrantyRepository.getAllWarranties().first()
                    .mapNotNull { it.receiptPhotoPath }
                    .toSet()
                OrphanPhotoSweeper(File(filesDir, OrphanPhotoSweeper.PHOTOS_DIR_NAME))
                    .sweep(referenced)
            }
        }
    }
}
