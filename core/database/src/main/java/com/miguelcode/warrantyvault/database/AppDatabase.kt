package com.miguelcode.warrantyvault.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.miguelcode.warrantyvault.database.converter.Converters
import com.miguelcode.warrantyvault.database.dao.WarrantyDao
import com.miguelcode.warrantyvault.database.entity.WarrantyEntity

@Database(
    entities = [WarrantyEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun warrantyDao(): WarrantyDao
}

/**
 * v2: seguimiento manual (en reclamación, reparada, archivada). Las columnas nuevas
 * llevan valor por defecto, así que las garantías existentes quedan sin seguimiento.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE warranties ADD COLUMN tracking TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE warranties ADD COLUMN trackingDate INTEGER")
    }
}
