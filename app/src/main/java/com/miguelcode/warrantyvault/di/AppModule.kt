package com.miguelcode.warrantyvault.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.miguelcode.warrantyvault.BuildConfig
import com.miguelcode.warrantyvault.analytics.NoOpAnalyticsTracker
import com.miguelcode.warrantyvault.backup.WarrantyBackupImpl
import com.miguelcode.warrantyvault.backup.WarrantyPdfExporter
import com.miguelcode.warrantyvault.domain.backup.WarrantyBackup
import com.miguelcode.warrantyvault.database.AppDatabase
import com.miguelcode.warrantyvault.database.MIGRATION_1_2
import com.miguelcode.warrantyvault.database.datasource.WarrantyLocalDataSource
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.database.repository.PreferencesRepositoryImpl
import com.miguelcode.warrantyvault.database.repository.WarrantyRepositoryImpl
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.AddWarrantyUseCase
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.DeleteWarrantyUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantiesUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantyByIdUseCase
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.domain.usecase.SetWarrantyTrackingUseCase
import com.miguelcode.warrantyvault.domain.usecase.UpdateWarrantyUseCase
import com.miguelcode.warrantyvault.billing.BillingManager
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import com.miguelcode.warrantyvault.notifications.DeliveredAlertsStore
import com.miguelcode.warrantyvault.notifications.NotificationSchedulerImpl
import com.miguelcode.warrantyvault.storage.OrphanPhotoSweeper
import com.miguelcode.warrantyvault.storage.PhotoFileCleanerImpl
import com.miguelcode.warrantyvault.feature.addedit.AddEditViewModel
import com.miguelcode.warrantyvault.feature.detail.DetailViewModel
import com.miguelcode.warrantyvault.feature.home.HomeViewModel
import com.miguelcode.warrantyvault.feature.onboarding.OnboardingViewModel
import com.miguelcode.warrantyvault.feature.premium.PremiumViewModel
import com.miguelcode.warrantyvault.feature.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.io.File

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "warranty_vault_db")
            .addMigrations(MIGRATION_1_2)
            .apply {
                // En debug aceptamos perder datos al cambiar el esquema durante el desarrollo.
                // En release nunca: las migraciones tienen que estar definidas (addMigrations)
                // antes de subir una versión con cambios de esquema, o la app crashea al abrirse.
                if (BuildConfig.DEBUG) fallbackToDestructiveMigration()
            }
            // SQLite deja el contenido de las filas borradas en las páginas libres hasta que
            // se reutilizan: una garantía borrada se podía recuperar con un volcado forense.
            // secure_delete sobrescribe con ceros al borrar. Es un PRAGMA que devuelve fila,
            // así que va por query() y no por execSQL().
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.query("PRAGMA secure_delete = ON").close()
                }
            })
            .build()
    }
    single { get<AppDatabase>().warrantyDao() }
    single { WarrantyLocalDataSource(get()) }
}

val repositoryModule = module {
    single<WarrantyRepository> { WarrantyRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(androidContext()) }
    single<BillingManagerContract> { BillingManager(androidContext(), get()) }
    single { DeliveredAlertsStore(androidContext()) }
    single<NotificationScheduler> { NotificationSchedulerImpl(androidContext(), get()) }
    single<PhotoFileCleaner> {
        PhotoFileCleanerImpl(File(androidContext().filesDir, OrphanPhotoSweeper.PHOTOS_DIR_NAME))
    }
    // NO cambiar a FirebaseAnalyticsTracker sin leer antes su KDoc: hay que actualizar
    // la política de privacidad y la ficha de Play primero.
    single<AnalyticsTracker> { NoOpAnalyticsTracker() }
    single { WarrantyPdfExporter(androidContext()) }
    single<WarrantyBackup> { WarrantyBackupImpl(androidContext(), get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { GetWarrantiesUseCase(get()) }
    factory { GetWarrantyByIdUseCase(get()) }
    factory { AddWarrantyUseCase(get(), get(), get(), get(), get()) }
    factory { UpdateWarrantyUseCase(get(), get(), get(), get(), get()) }
    factory { DeleteWarrantyUseCase(get(), get(), get(), get()) }
    factory { CheckPremiumLimitUseCase(get(), get()) }
    factory { SetWarrantyTrackingUseCase(get(), get(), get(), get()) }
    factory { SyncExpirationAlertsUseCase(get(), get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { parameters ->
        DetailViewModel(
            warrantyId = parameters.get(),
            warrantyRepository = get(),
            deleteWarrantyUseCase = get(),
            setTrackingUseCase = get()
        )
    }
    viewModel { parameters ->
        AddEditViewModel(
            warrantyId = parameters.getOrNull(),
            getWarrantyByIdUseCase = get(),
            addWarrantyUseCase = get(),
            updateWarrantyUseCase = get(),
            checkPremiumLimitUseCase = get(),
            // Koin lo construye desde los CreationExtras del ViewModel, no de una
            // definición del módulo. Es lo que permite recuperar el formulario cuando
            // el sistema mata el proceso con la pantalla abierta.
            savedStateHandle = parameters.getOrNull()
        )
    }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { PremiumViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(databaseModule, repositoryModule, useCaseModule, viewModelModule)
